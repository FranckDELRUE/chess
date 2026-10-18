"""Shared helpers for the S0-7 benchmarks: positions, results files, GPU memory."""

from __future__ import annotations

import json
import statistics
import subprocess
import time
from pathlib import Path

from station.config import REPO_ROOT, StationConfig

# The 10 middlegames of S0-4 (`app/.../enginedebug/BenchmarkPositions.kt`), so PC and tablet compare.
SF_FENS = [
    "r2q1rk1/pp2bppp/2n1pn2/3p4/3P4/2N1PN2/PP2BPPP/R2Q1RK1 w - - 0 10",
    "r1bq1rk1/pp2ppbp/2np1np1/8/3NP3/2N1BP2/PPPQ2PP/R3KB1R w KQ - 2 9",
    "r1bqkb1r/pp3ppp/2n1pn2/2pp4/2PP4/2N2N2/PP2PPPP/R1BQKB1R w KQkq - 0 6",
    "2rq1rk1/pp1bppbp/3p1np1/8/3NP3/2N1BP2/PPPQ2PP/2KR1B1R w - - 0 11",
    "r3k2r/pp1nbppp/2p1pn2/q7/2PP4/2N1PN2/PPQ2PPP/R1B1KB1R w KQkq - 3 9",
    "r1b2rk1/ppq1bppp/2n1pn2/2pp4/3P1B2/2PBPN2/PP1N1PPP/R2Q1RK1 w - - 0 10",
    "rnbq1rk1/pp2bppp/4pn2/2pp4/2PP4/2N2NP1/PP2PPBP/R1BQ1RK1 b - - 0 7",
    "r4rk1/1pp1qppp/p1np1n2/2b1p1B1/2B1P1b1/P1NP1N2/1PP1QPPP/R4RK1 w - - 0 10",
    "2r2rk1/pp2qppp/2n1p3/3pP3/3P4/P1N1B3/1PQ2PPP/R4RK1 w - - 0 15",
    "r1bq1rk1/ppp2ppp/2n2n2/3pp3/1bP5/2NP1NP1/PP2PPBP/R1BQ1RK1 w - - 0 7",
]

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
