from bench.uci import Info, SearchResult, parse_info


def test_parse_stockfish_info_line() -> None:
    info = parse_info(
        "info depth 30 seldepth 41 multipv 1 score cp 23 nodes 912345678 nps 31000000 hashfull 512 "
        "tbhits 0 time 29430 pv e2e4 e7e5 g1f3"
    )
    assert info == Info(
        depth=30, nodes=912345678, nps=31000000, time_ms=29430, score_cp=23, pv=("e2e4", "e7e5", "g1f3")
    )


def test_parse_mate_score() -> None:
    info = parse_info("info depth 12 score mate -3 nodes 10 time 1 pv h7h8")
    assert info is not None and info.score_mate == -3 and info.score_cp is None


def test_non_search_lines_are_ignored() -> None:
    assert parse_info("info string NNUE evaluation using nn-1a298aa575a0.nnue") is None
    assert parse_info("info currmove e2e4 currmovenumber 1") is None
    assert parse_info("bestmove e2e4 ponder e7e5") is None


def test_time_to_depth_uses_first_line_at_that_depth() -> None:
    result = SearchResult(
        "e2e4",
        5000.0,
        [Info(depth=20, time_ms=900), Info(depth=20, time_ms=950), Info(depth=25, time_ms=3000)],
    )
    assert result.wall_ms_to_depth(20) == 900
    assert result.wall_ms_to_depth(30) is None
    assert result.last == Info(depth=25, time_ms=3000)
