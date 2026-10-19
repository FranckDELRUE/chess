"""Shared helpers for the benchmarks (S0-7, S2-2): positions, results files, GPU memory."""

from __future__ import annotations

import json
import statistics
import subprocess
import time
from pathlib import Path

from station.config import REPO_ROOT, StationConfig

HEIDOJI_CANDIDATES = REPO_ROOT / "testdata/positions/heidoji-candidates.txt"
# The 20 middlegames of S2-2, shared with the tablet (`BenchmarkPositions.FENS`) so PC and tablet compare.
HEIDOJI_MIDDLEGAMES = REPO_ROOT / "testdata/positions/heidoji-middlegames.txt"


def read_positions(path: Path, keep_comment: bool = False) -> list[str]:
    """Non-blank, non-`#` lines; without [keep_comment], only the FEN before ` ;`."""
    lines = [line.strip() for line in path.read_text(encoding="utf-8").splitlines()]
    entries = [line for line in lines if line and not line.startswith("#")]
    return entries if keep_comment else [line.split(" ;")[0] for line in entries]


def sf_fens() -> list[str]:
    """Stockfish benchmark positions (S2-2; S0-4 used 10 hand-picked ones, see rapport-s0.md)."""
    return read_positions(HEIDOJI_MIDDLEGAMES)


MAIA_POSITIONS = REPO_ROOT / "core/engine/src/test/resources/maia-positions.txt"


def maia_fens() -> list[str]:
    """The 20 positions of S0-5 (shared with the lc0 reference test)."""
    return [line.strip() for line in MAIA_POSITIONS.read_text(encoding="utf-8").splitlines() if line.strip()]


def save_results(name: str, data: dict, config: StationConfig | None = None) -> Path:
    out_dir = (config or StationConfig()).results_dir
    out_dir.mkdir(parents=True, exist_ok=True)
    path = out_dir / f"{name}-{time.strftime('%Y%m%d-%H%M%S')}.json"
    path.write_text(json.dumps(data, ensure_ascii=False, indent=2), encoding="utf-8")
    return path


def gpu_memory_used_mb() -> int | None:
    """Total GPU memory in use (all processes), from nvidia-smi."""
    try:
        out = subprocess.run(
            ["nvidia-smi", "--query-gpu=memory.used", "--format=csv,noheader,nounits"],
            capture_output=True,
            text=True,
            timeout=5,
            check=True,
        )
        return int(out.stdout.strip().splitlines()[0])
    except (OSError, subprocess.SubprocessError, ValueError, IndexError):
        return None


def median(values: list[float]) -> float:
    return statistics.median(values) if values else float("nan")


def percentile(values: list[float], p: float) -> float:
    if not values:
        return float("nan")
    ordered = sorted(values)
    return ordered[min(len(ordered) - 1, int(len(ordered) * p))]
