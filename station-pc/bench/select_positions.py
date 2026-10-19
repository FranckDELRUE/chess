"""S2-2 stage 2: keep balanced candidates as the 20 benchmark middlegames.

Reads `testdata/positions/heidoji-candidates.txt` (stage 1, `HeidojiSelection` in `:app` tests),
evaluates them in order with Stockfish at depth 18 on one thread (deterministic), keeps the first
candidate of each game whose evaluation is within +/-150 centipawns (no mate), and writes the first
20 kept to `heidoji-middlegames.txt`, unchanged.

Usage: python -m bench.select_positions
"""

from __future__ import annotations

from bench.common import HEIDOJI_CANDIDATES, HEIDOJI_MIDDLEGAMES, read_positions
from bench.uci import UciEngine
from station.config import StationConfig

DEPTH = 18
MAX_CP = 150
COUNT = 20
HASH_MB = 64

HEADER = (
    "# S2-2 benchmark middlegames (bench/select_positions.py): from heidoji-candidates.txt, the first candidate "
    f"of each game within +/-{MAX_CP} cp at depth {DEPTH} (Stockfish 19, 1 thread, Hash {HASH_MB} MB), "
    f"for the first {COUNT} such games. Generated, do not edit."
)


def run() -> list[str]:
    kept: list[str] = []
    games_kept: set[str] = set()
    with UciEngine(StationConfig().stockfish) as engine:
        engine.set_option("Threads", 1)
        engine.set_option("Hash", HASH_MB)
        engine.ready()
        for line in read_positions(HEIDOJI_CANDIDATES, keep_comment=True):
            fen, game = line.split(" ;")[0], line.split("; game ")[1].split(" ply")[0]
            if game in games_kept:
                continue
            engine.new_game()
            last = engine.go(fen, f"depth {DEPTH}").last
            balanced = last is not None and last.score_mate is None and abs(last.score_cp or 0) <= MAX_CP
            score = f"mate {last.score_mate}" if last and last.score_mate is not None else f"cp {last.score_cp if last else None}"
            print(f"{'keep' if balanced else 'drop'} {score:>10}  {line}")
            if balanced:
                kept.append(line)
                games_kept.add(game)
                if len(kept) == COUNT:
                    break
    if len(kept) < COUNT:
        raise SystemExit(f"only {len(kept)} balanced candidates, {COUNT} needed: revise stage 1")
    HEIDOJI_MIDDLEGAMES.write_text("\n".join([HEADER, *kept]) + "\n", encoding="utf-8")
    return kept


if __name__ == "__main__":
    print("saved", len(run()), "positions to", HEIDOJI_MIDDLEGAMES)
