"""Minimal UCI client over a subprocess, enough for benchmarks (Stockfish, lc0)."""

from __future__ import annotations

import subprocess
import time
from dataclasses import dataclass
from pathlib import Path


@dataclass
class Info:
    depth: int | None = None
    nodes: int | None = None
    nps: int | None = None
    time_ms: int | None = None
    score_cp: int | None = None
    score_mate: int | None = None
    pv: tuple[str, ...] = ()


def parse_info(line: str) -> Info | None:
    """Parses an `info` line with a depth; returns None for other lines (strings, currmove...)."""
    tokens = line.split()
    if not tokens or tokens[0] != "info" or "depth" not in tokens or "string" in tokens:
        return None
    info = Info()
    ints = {"depth": "depth", "nodes": "nodes", "nps": "nps", "time": "time_ms"}
    i = 1
    while i < len(tokens):
        key = tokens[i]
        if key in ints and i + 1 < len(tokens):
            setattr(info, ints[key], int(tokens[i + 1]))
            i += 2
        elif key == "score" and i + 2 < len(tokens):
            if tokens[i + 1] == "cp":
                info.score_cp = int(tokens[i + 2])
            elif tokens[i + 1] == "mate":
                info.score_mate = int(tokens[i + 2])
            i += 3
        elif key == "pv":
            info.pv = tuple(tokens[i + 1 :])
            break
        else:
            i += 1
    return info


@dataclass
class SearchResult:
    best_move: str
    wall_ms: float
    infos: list[Info]

    @property
    def last(self) -> Info | None:
        return self.infos[-1] if self.infos else None

    def wall_ms_to_depth(self, depth: int) -> int | None:
        """Engine-reported time of the first info line at [depth] (Stockfish reports it in ms)."""
        return next((i.time_ms for i in self.infos if i.depth == depth and i.time_ms is not None), None)


class UciEngine:
    def __init__(self, exe: Path, args: list[str] | None = None, cwd: Path | None = None) -> None:
        self.proc = subprocess.Popen(
            [str(exe), *(args or [])],
            stdin=subprocess.PIPE,
            stdout=subprocess.PIPE,
            stderr=subprocess.DEVNULL,
            text=True,
            bufsize=1,
            cwd=cwd or exe.parent,
        )
        self.name = ""
        self.send("uci")
        for line in self._read_until("uciok"):
            if line.startswith("id name "):
                self.name = line.removeprefix("id name ")

    def send(self, line: str) -> None:
        assert self.proc.stdin is not None
        self.proc.stdin.write(line + "\n")
        self.proc.stdin.flush()

    def _read_until(self, prefix: str) -> list[str]:
        assert self.proc.stdout is not None
        lines = []
        for raw in self.proc.stdout:
            line = raw.strip()
            lines.append(line)
            if line.startswith(prefix):
                return lines
        raise RuntimeError(f"Engine exited before {prefix!r}")

    def set_option(self, name: str, value: object) -> None:
        self.send(f"setoption name {name} value {value}")

    def ready(self) -> None:
        self.send("isready")
        self._read_until("readyok")

    def new_game(self) -> None:
        self.send("ucinewgame")
        self.ready()

    def go(self, fen: str, limit: str) -> SearchResult:
        """Runs `go <limit>` (e.g. "depth 30", "nodes 1") and returns the bestmove with its info lines."""
        self.send(f"position fen {fen}")
        start = time.perf_counter()
        self.send(f"go {limit}")
        lines = self._read_until("bestmove")
        wall_ms = (time.perf_counter() - start) * 1000
        infos = [i for i in (parse_info(line) for line in lines) if i is not None]
        return SearchResult(lines[-1].split()[1], wall_ms, infos)

    @property
    def pid(self) -> int:
        return self.proc.pid

    def close(self) -> None:
        try:
            self.send("quit")
            self.proc.wait(timeout=10)
        except (OSError, subprocess.TimeoutExpired):
            self.proc.kill()

    def __enter__(self) -> UciEngine:
        return self

    def __exit__(self, *exc: object) -> None:
        self.close()
