"""Converts the Maia-1 networks to ONNX and records lc0's own policy as a test reference.

Run scripts/fetch-maia-nets.ps1 first. Standard library only (Python 3.10+):

    python tools/maia2onnx/maia2onnx.py

Outputs:
- core/engine/src/main/assets/maia-<elo>.onnx (not versioned, see .gitignore)
- core/engine/src/test/resources/maia-reference.tsv: for each net and test position, every legal
  move with lc0's policy index and prior (softmax temperature 1, default history fill `fen_only`, 1 node).
  MaiaPipelineTest compares the Kotlin encoder + ONNX Runtime against it.
"""

from __future__ import annotations

import argparse
import re
import subprocess
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
NETS = ["1100", "1500", "1900"]
MOVE_STAT = re.compile(r"^info string (\S+)\s+\(\s*(\d+)\s*\).*\(P:\s*([\d.]+)%\)")
# lc0 prints castling as "king takes rook"; the reference uses standard UCI.
CASTLING = {"e1h1": "e1g1", "e1a1": "e1c1", "e8h8": "e8g8", "e8a8": "e8c8"}


def king_on(fen: str, square: str) -> bool:
    rank = fen.split()[0].split("/")[8 - int(square[1])]
    files = "".join("." * int(c) if c.isdigit() else c for c in rank)
    return files["abcdefgh".index(square[0])] in "Kk"


def convert(lc0: Path, net: Path, onnx: Path) -> None:
    subprocess.run(
        [str(lc0), "leela2onnx", f"--input={net}", f"--output={onnx}"],
        check=True,
        capture_output=True,
    )
    print(f"converted {net.name} -> {onnx.relative_to(ROOT)} ({onnx.stat().st_size} bytes)")


class Lc0:
    def __init__(self, lc0: Path, net: Path) -> None:
        self.proc = subprocess.Popen(
            [
                str(lc0),
                f"--weights={net}",
                "--policy-softmax-temp=1.0",
                "--verbose-move-stats",
                "--threads=1",
            ],
            stdin=subprocess.PIPE,
            stdout=subprocess.PIPE,
            stderr=subprocess.DEVNULL,
            text=True,
        )
        self.send("uci")
        self.read_until("uciok")

    def send(self, line: str) -> None:
        assert self.proc.stdin is not None
        self.proc.stdin.write(line + "\n")
        self.proc.stdin.flush()

    def read_until(self, prefix: str) -> list[str]:
        assert self.proc.stdout is not None
        lines = []
        while True:
            line = self.proc.stdout.readline()
            if not line:
                raise RuntimeError(f"lc0 stopped before '{prefix}'")
            line = line.strip()
            lines.append(line)
            if line.startswith(prefix):
                return lines

    def policy(self, fen: str) -> list[tuple[str, int, float]]:
        self.send("ucinewgame")
        self.send(f"position fen {fen}")
        self.send("go nodes 1")
        stats = []
        for line in self.read_until("bestmove"):
            match = MOVE_STAT.match(line)
            if match and match.group(1) != "node":
                move = match.group(1)
                if move in CASTLING and king_on(fen, move[:2]):
                    move = CASTLING[move]
                stats.append((move, int(match.group(2)), float(match.group(3)) / 100))
        return sorted(stats, key=lambda s: -s[2])

    def close(self) -> None:
        self.send("quit")
        self.proc.wait(timeout=10)


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--work", type=Path, default=ROOT / "build" / "maia")
    args = parser.parse_args()
    lc0 = args.work / "lc0" / "lc0.exe"
    assets = ROOT / "core" / "engine" / "src" / "main" / "assets"
    resources = ROOT / "core" / "engine" / "src" / "test" / "resources"
    assets.mkdir(parents=True, exist_ok=True)
    fens = [f for f in (resources / "maia-positions.txt").read_text().splitlines() if f.strip()]

    rows = ["net\tfen\tmove\tindex\tprior"]
    for elo in NETS:
        net = args.work / f"maia-{elo}.pb.gz"
        convert(lc0, net, assets / f"maia-{elo}.onnx")
        engine = Lc0(lc0, net)
        for fen in fens:
            stats = engine.policy(fen)
            if not stats:
                raise RuntimeError(f"no move stats for {fen}")
            rows += [f"{elo}\t{fen}\t{move}\t{index}\t{prior:.4f}" for move, index, prior in stats]
        engine.close()
        print(f"reference maia-{elo}: {len(fens)} positions")
    (resources / "maia-reference.tsv").write_text("\n".join(rows) + "\n", newline="\n")


if __name__ == "__main__":
    main()
