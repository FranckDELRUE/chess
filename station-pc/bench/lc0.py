"""S0-7: lc0 with the CUDA backend and the Maia networks, time per move (policy only, `go nodes 1`).

Same 20 positions as S0-5 on the tablet. The CPU build of S0-5 (build/maia/lc0, OpenBLAS) is the baseline.
Usage: python -m bench.lc0 [--rounds 5] [--no-cpu]
"""

from __future__ import annotations

import argparse
import time
from pathlib import Path

from bench.common import gpu_memory_used_mb, maia_fens, median, percentile, save_results
from bench.uci import UciEngine
from station.config import BUILD_DIR, StationConfig

CPU_LC0 = BUILD_DIR / "maia/lc0/lc0.exe"


def measure(exe: Path, backend: str, net: str, weights: Path, rounds: int) -> dict:
    fens = maia_fens()
    vram_before = gpu_memory_used_mb()
    t0 = time.perf_counter()
    with UciEngine(exe, [f"--weights={weights}", f"--backend={backend}"]) as engine:
        engine.ready()
        engine.new_game()
        cold = engine.go(fens[0], "nodes 1")
        start_ms = (time.perf_counter() - t0) * 1000
        vram_loaded = gpu_memory_used_mb()
        times: list[float] = []
        moves: list[str] = []
        for r in range(rounds):
            for fen in fens:
                engine.new_game()  # no tree reuse between rounds
                result = engine.go(fen, "nodes 1")
                times.append(result.wall_ms)
                if r == 0:
                    moves.append(result.best_move)
    row = {
        "backend": backend,
        "net": net,
        "startup_to_first_move_ms": round(start_ms, 1),
        "cold_move_ms": round(cold.wall_ms, 1),
        "median_ms": round(median(times), 2),
        "p90_ms": round(percentile(times, 0.9), 2),
        "vram_delta_mb": (vram_loaded - vram_before) if vram_before is not None and vram_loaded is not None else None,
        "moves": moves,
    }
    print({k: v for k, v in row.items() if k != "moves"})
    return row


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--rounds", type=int, default=5)
    parser.add_argument("--no-cpu", action="store_true")
    args = parser.parse_args()
    config = StationConfig()
    configs = [(config.lc0, "cuda-auto"), (config.lc0, "cuda-fp16")]
    if not args.no_cpu and CPU_LC0.is_file():
        configs.append((CPU_LC0, "blas"))
    rows = []
    for net in config.maia_nets():
        weights = config.maia_dir / f"maia-{net}.pb.gz"
        rows += [measure(exe, backend, net, weights, args.rounds) for exe, backend in configs]
    # Agreement between backends on the chosen move (fp16 rounding may flip close calls).
    for net in config.maia_nets():
        by_backend = {r["backend"]: r["moves"] for r in rows if r["net"] == net}
        ref = by_backend.get("cuda-auto")
        for backend, moves in by_backend.items():
            if ref and backend != "cuda-auto":
                same = sum(a == b for a, b in zip(ref, moves, strict=True))
                print(f"net={net} {backend} vs cuda-auto: same move {same}/{len(ref)}")
    print("saved", save_results("lc0", {"engine": "lc0", "rounds": args.rounds, "rows": rows}))


if __name__ == "__main__":
    main()
