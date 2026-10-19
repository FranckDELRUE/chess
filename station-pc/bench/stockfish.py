"""S0-7, S2-2: Stockfish 19 x86-64 on all cores, time to reach depth 30 on the benchmark middlegames.

Usage: python -m bench.stockfish [--threads 32] [--hash 2048] [--depth 30]
"""

from __future__ import annotations

import argparse
import os

from bench.common import median, save_results, sf_fens
from bench.uci import UciEngine
from station.config import StationConfig

MILESTONES = (20, 25, 30)


def run(threads: int, hash_mb: int, depth: int) -> dict:
    config = StationConfig()
    rows = []
    with UciEngine(config.stockfish) as engine:
        engine.set_option("Threads", threads)
        engine.set_option("Hash", hash_mb)
        engine.ready()
        print(f"{engine.name} threads={threads} hash={hash_mb}MB depth={depth}")
        for i, fen in enumerate(sf_fens(), start=1):
            engine.new_game()
            result = engine.go(fen, f"depth {depth}")
            last = result.last
            row = {
                "pos": i,
                "wall_ms": round(result.wall_ms),
                **{f"d{d}_ms": result.wall_ms_to_depth(d) for d in MILESTONES if d <= depth},
                "nodes": last.nodes if last else None,
                "nps": last.nps if last else None,
                "score_cp": last.score_cp if last else None,
                "score_mate": last.score_mate if last else None,
                "best": result.best_move,
            }
            rows.append(row)
            print(row)
    summary = {
        "median_wall_ms": median([r["wall_ms"] for r in rows]),
        "mean_wall_ms": sum(r["wall_ms"] for r in rows) / len(rows),
        "median_nps": median([r["nps"] for r in rows if r["nps"]]),
        **{f"median_d{d}_ms": median([r[f"d{d}_ms"] for r in rows if r.get(f"d{d}_ms")]) for d in MILESTONES},
    }
    print(summary)
    return {"engine": "stockfish", "threads": threads, "hash_mb": hash_mb, "depth": depth, "rows": rows, **summary}


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--threads", type=int, default=os.cpu_count() or 1)
    parser.add_argument("--hash", type=int, default=2048)
    parser.add_argument("--depth", type=int, default=30)
    args = parser.parse_args()
    data = run(args.threads, args.hash, args.depth)
    print("saved", save_results(f"stockfish-t{args.threads}", data))


if __name__ == "__main__":
    main()
