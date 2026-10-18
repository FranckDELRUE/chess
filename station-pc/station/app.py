"""FastAPI entry point. S0-7 only exposes /health; analysis routes come with S9."""

from __future__ import annotations

import shutil
import subprocess
from functools import lru_cache

import httpx
from fastapi import Depends, FastAPI

from station import __version__
from station.config import StationConfig

app = FastAPI(title="Chess Companion station", version=__version__)


@lru_cache(maxsize=1)
def get_config() -> StationConfig:
    return StationConfig()


@lru_cache(maxsize=1)
def gpu_name() -> str | None:
    """Name of the first NVIDIA GPU, read once (nvidia-smi takes ~100 ms)."""
    if shutil.which("nvidia-smi") is None:
        return None
    try:
        out = subprocess.run(
            ["nvidia-smi", "--query-gpu=name", "--format=csv,noheader"],
            capture_output=True,
            text=True,
            timeout=5,
            check=True,
        )
    except (OSError, subprocess.SubprocessError):
        return None
    lines = out.stdout.strip().splitlines()
    return lines[0] if lines else None


def ollama_models(url: str) -> list[str] | None:
    """Installed Ollama models, or None if Ollama does not answer."""
    try:
        response = httpx.get(f"{url}/api/tags", timeout=1.0)
        response.raise_for_status()
    except httpx.HTTPError:
        return None
    return sorted(m["name"] for m in response.json().get("models", []))


@app.get("/health")
def health(config: StationConfig = Depends(get_config)) -> dict:
    """Liveness plus what the station can do right now; the tablet reads it before offloading work."""
    models = ollama_models(config.ollama_url)
    return {
        "status": "ok",
        "version": __version__,
        "gpu": gpu_name(),
        "engines": {
            "stockfish": config.stockfish.is_file(),
            "lc0": config.lc0.is_file(),
            "maia": config.maia_nets(),
        },
        "ollama": {"reachable": models is not None, "models": models or []},
    }
