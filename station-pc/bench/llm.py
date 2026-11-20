"""S0-7: teacher LLMs on the GPU through Ollama, same prompts and facts as the S0-6 tablet spike.

Per model: load time and VRAM, then 2 prompt variants x 5 facts x [runs] sampled answers with
time to first token, prompt and generation speed, the text and its fidelity to the fact.
Usage: python -m bench.llm [--models qwen3:14b mistral-nemo:12b] [--runs 3]
"""

from __future__ import annotations

import argparse
import json
import re
import time

import httpx

from bench import fidelity, prompts
from bench.common import gpu_memory_used_mb, median, save_results
from station.config import StationConfig

DEFAULT_MODELS = ["qwen3:14b", "mistral-nemo:12b"]
# Same sampling as S0-6 (LlamaTextGenerator defaults); a larger context than the tablet's 1024 is harmless.
OPTIONS = {"temperature": 0.7, "top_k": 40, "top_p": 0.9, "num_predict": 96, "num_ctx": 2048}
_THINK = re.compile(r"<think>.*?(</think>|$)", re.DOTALL)


def chat(client: httpx.Client, url: str, model: str, messages: list[dict], options: dict) -> dict:
    """Streams one answer; returns text, wall-clock time to first token and Ollama's own counters."""
    body = {"model": model, "messages": messages, "options": options, "stream": True, "keep_alive": "10m"}
    if model.startswith("qwen3"):
        body["think"] = False  # Qwen3 reasoning off, as on the tablet
    text, first_ms, final = [], None, {}
    start = time.perf_counter()
    with client.stream("POST", f"{url}/api/chat", json=body, timeout=300) as response:
        response.raise_for_status()
        for line in response.iter_lines():
            if not line:
                continue
            chunk = json.loads(line)
            piece = chunk.get("message", {}).get("content", "")
            if piece and first_ms is None:
                first_ms = (time.perf_counter() - start) * 1000
            text.append(piece)
            if chunk.get("done"):
                final = chunk
    ns = 1e9
    prompt_s = final.get("prompt_eval_duration", 0) / ns
    eval_s = final.get("eval_duration", 0) / ns
    return {
        "text": _THINK.sub("", "".join(text)).strip(),
        "ttft_ms": round(first_ms or float("nan"), 1),
        "total_ms": round((time.perf_counter() - start) * 1000, 1),
        "load_ms": round(final.get("load_duration", 0) / 1e6, 1),
        "prompt_tokens": final.get("prompt_eval_count", 0),
        "prompt_tok_s": round(final.get("prompt_eval_count", 0) / prompt_s, 1) if prompt_s else None,
        "gen_tokens": final.get("eval_count", 0),
        "gen_tok_s": round(final.get("eval_count", 0) / eval_s, 1) if eval_s else None,
    }


def unload(client: httpx.Client, url: str, model: str) -> None:
    """Unloads [model] and waits until Ollama no longer lists it (freeing VRAM is asynchronous)."""
    client.post(f"{url}/api/generate", json={"model": model, "keep_alive": 0}, timeout=60)
    for _ in range(60):
        running = client.get(f"{url}/api/ps", timeout=10).json().get("models", [])
        if not any(m.get("name") == model or m.get("model") == model for m in running):
            break
        time.sleep(0.5)
    time.sleep(2)  # the runner process exits shortly after


def vram_of(client: httpx.Client, url: str, model: str) -> dict:
    running = client.get(f"{url}/api/ps", timeout=10).json().get("models", [])
    entry = next((m for m in running if m.get("name") == model or m.get("model") == model), {})
    return {"size_mb": entry.get("size", 0) // 2**20, "size_vram_mb": entry.get("size_vram", 0) // 2**20}


def bench_model(client: httpx.Client, url: str, model: str, runs: int) -> dict:
    vram_idle = gpu_memory_used_mb()
    cold = chat(client, url, model, prompts.messages(prompts.FACTS[0], "JSON"), {**OPTIONS, "temperature": 0})
    loaded = {"cold": cold, "gpu_used_idle_mb": vram_idle, "gpu_used_loaded_mb": gpu_memory_used_mb()}
    loaded.update(vram_of(client, url, model))
    print(model, {k: v for k, v in loaded.items() if k != "cold"}, "cold", {k: v for k, v in cold.items() if k != "text"})
    answers = []
    for variant in prompts.VARIANTS:
        for i, fact in enumerate(prompts.FACTS, start=1):
            for run in range(1, runs + 1):
                a = chat(client, url, model, prompts.messages(fact, variant), {**OPTIONS, "seed": run})
                report = fidelity.check(fact.json, a["text"])
                a.update(
                    variant=variant,
                    fact=i,
                    run=run,
                    faithful=report.faithful,
                    best=report.mentions_best,
                    piece=report.names_piece,
                    sentences=report.sentences,
                    invented=report.invented,
                )
                answers.append(a)
                print(f"{model} {variant} fact={i} run={run} ttft={a['ttft_ms']}ms gen={a['gen_tok_s']}tok/s "
                      f"faithful={report.faithful} invented={report.invented} | {a['text']}")
    summaries = {}
    for variant in prompts.VARIANTS:
        rows = [a for a in answers if a["variant"] == variant]
        summaries[variant] = {
            "n": len(rows),
            "faithful": sum(a["faithful"] for a in rows),
            "best": sum(a["best"] for a in rows),
            "one_sentence": sum(a["sentences"] == 1 for a in rows),
            "ttft_median_ms": median([a["ttft_ms"] for a in rows]),
            "total_median_ms": median([a["total_ms"] for a in rows]),
            "prompt_tok_s_median": median([a["prompt_tok_s"] for a in rows if a["prompt_tok_s"]]),
            "gen_tok_s_median": median([a["gen_tok_s"] for a in rows if a["gen_tok_s"]]),
            "gen_tokens_median": median([a["gen_tokens"] for a in rows]),
        }
        print(model, variant, summaries[variant])
    unload(client, url, model)
    return {"model": model, **loaded, "summaries": summaries, "answers": answers}


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--models", nargs="+", default=DEFAULT_MODELS)
    parser.add_argument("--runs", type=int, default=3)
    args = parser.parse_args()
    url = StationConfig().ollama_url
    with httpx.Client() as client:
        for model in args.models:
            unload(client, url, model)  # start each model cold
        results = [bench_model(client, url, model, args.runs) for model in args.models]
    print("saved", save_results("llm", {"options": OPTIONS, "results": results}))


if __name__ == "__main__":
    main()
