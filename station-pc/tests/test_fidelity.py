"""Same cases as `spike/llm/.../FactFidelityTest.kt`: the Python port must judge like the Kotlin one."""

from bench import fidelity, prompts

FACT = prompts.FACTS[0].json


def test_faithful_sentence_with_the_french_move() -> None:
    r = fidelity.check(FACT, "Ton cavalier en f3 n'est pas défendu : joue Cd2 pour le mettre à l'abri !")
    assert r.faithful and r.mentions_best and r.names_piece


def test_english_move_and_square_from_the_move_are_accepted() -> None:
    r = fidelity.check(FACT, "Le cavalier f3 est en prise, Nd2 le ramène en d2.")
    assert r.faithful and r.mentions_best


def test_invented_square_and_move_are_reported() -> None:
    r = fidelity.check(FACT, "Ton fou en c4 est en prise, joue Fe2.")
    assert r.invented_squares == {"c4"}
    assert r.invented_moves == {"Fe2"}
    assert not r.faithful and not r.mentions_best and not r.names_piece


def test_check_and_mate_suffixes_are_ignored() -> None:
    assert fidelity.check(prompts.FACTS[1].json, "Cc7 fourchette le roi en e8 et la tour en a8.").mentions_best
    assert fidelity.check(prompts.FACTS[3].json, "Te8# : mat du couloir !").faithful


def test_wrong_piece_is_reported() -> None:
    r = fidelity.check(FACT, "Tu as bien vu, tu as capturé la dame en f3, et c'est une pièce en prise !")
    assert r.invented_pieces == {"dame"}
    assert not r.faithful and not r.names_piece


def test_turn_is_not_a_rook_and_a_mate_implies_the_king() -> None:
    assert fidelity.check(FACT, "À ton tour : ton cavalier f3 est seul, joue Cd2.").faithful
    assert fidelity.check(prompts.FACTS[3].json, "Ta tour en e8 fait mat au roi : Te8# !").faithful
    assert fidelity.check(FACT, "La tour est en prise.").invented_pieces == {"tour"}


def test_pieces_named_anywhere_in_the_fact_and_of_the_best_move_are_allowed() -> None:
    pin = prompts.FACTS[2].json
    assert fidelity.check(pin, "Ton fou en g5 cloue le cavalier f6 devant la dame d8 : prends avec Fxf6.").faithful
    free = prompts.FACTS[4].json
    assert fidelity.check(free, "Le fou en d4 est gratuit : prends-le avec ton cavalier, Cxd4 !").faithful


def test_sentences_are_counted() -> None:
    assert fidelity.count_sentences("Ton cavalier en f3 est en prise, joue Cd2 !") == 1
    assert fidelity.count_sentences("Bravo. Joue Cd2 ! C'est solide…") == 3
    assert fidelity.count_sentences("  ") == 0


def test_to_french() -> None:
    assert [fidelity.to_french(m) for m in ("Nd2", "Bxf6", "Re8#", "e4")] == ["Cd2", "Fxf6", "Te8#", "e4"]


def test_french_facts_are_faithful_to_their_json() -> None:
    for fact in prompts.FACTS:
        r = fidelity.check(fact.json, fact.french)
        assert r.faithful and r.mentions_best, (fact.french, r)


def test_prompts_carry_the_fact() -> None:
    for fact in prompts.FACTS:
        assert prompts.messages(fact, "JSON")[1]["content"].endswith(fact.json)
        assert fact.french in prompts.messages(fact, "FRENCH")[1]["content"]
