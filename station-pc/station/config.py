"""Where the station finds its engines and services. Each path can be overridden by an environment variable."""

from __future__ import annotations

import os
from dataclasses import dataclass, field
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[2]
BUILD_DIR = REPO_ROOT / "build"


def _path(env: str, default: Path) -> Path:
    value = os.environ.get(env)
    return Path(value) if value else default


@dataclass(frozen=True)
class StationConfig:
    stockfish: Path = field(
        default_factory=lambda: _path(
            "STATION_STOCKFISH",
            BUILD_DIR / "station/stockfish/stockfish/stockfish-windows-x86-64-universal.exe",
        )
    )
    lc0: Path = field(default_factory=lambda: _path("STATION_LC0", BUILD_DIR / "station/lc0-cuda12/lc0.exe"))
    maia_dir: Path = field(default_factory=lambda: _path("STATION_MAIA_DIR", BUILD_DIR / "maia"))
    ollama_url: str = field(default_factory=lambda: os.environ.get("STATION_OLLAMA_URL", "http://127.0.0.1:11434"))
    results_dir: Path = field(default_factory=lambda: _path("STATION_RESULTS", BUILD_DIR / "station/results"))

    def maia_nets(self) -> list[str]:
        """Maia levels available as lc0 weights (maia-1100.pb.gz -> "1100")."""
        if not self.maia_dir.is_dir():
            return []
        return sorted(p.name.removeprefix("maia-").removesuffix(".pb.gz") for p in self.maia_dir.glob("maia-*.pb.gz"))
