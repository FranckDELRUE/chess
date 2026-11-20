"""Port of `FactFidelity.kt` (S0-6): squares, moves and pieces a teacher sentence adds to its fact.

Heuristic: it reads words, not meaning. Kept identical to the Kotlin version so both spikes compare.
"""

from __future__ import annotations

import re
from dataclasses import dataclass

_SQUARE = re.compile(r"(?<![a-zA-Z0-9])[a-h][1-8](?![0-9])")
_ANY_SQUARE = re.compile(r"[a-h][1-8]")
_MOVE = re.compile(r"(?<![A-Za-z0-9])[KQRBNDTFC]x?[a-h][1-8][+#]?")
_PIECE = re.compile(r'"piece"\s*:\s*"(\w+)"')
_BEST = re.compile(r'"best"\s*:\s*"([^"]+)"')
_ENGLISH_PIECE = re.compile(r"\b(king|queen|rook|bishop|knight|pawn)\b")
# "tour" after a possessive is a turn ("à ton tour"), not a rook. Python lookbehinds need a fixed width.
_FRENCH_PIECE_WORD = re.compile(
    r"(?<![^\W\d_])(roi|dame|(?<!ton )(?<!mon )(?<!son )(?<!chaque )(?<!premier )tour|fou|cavalier|pion)s?(?![^\W\d_])",
    re.IGNORECASE,
)
_SENTENCE_END = re.compile(r"[.!?…]+(?=\s|$)")

_FRENCH_LETTER = {"K": "R", "Q": "D", "R": "T", "B": "F", "N": "C"}
_PIECE_OF_LETTER = {"K": "roi", "Q": "dame", "R": "tour", "B": "fou", "N": "cavalier"}
_FRENCH_PIECE = {
    "king": "roi",
    "queen": "dame",
    "rook": "tour",
    "bishop": "fou",
    "knight": "cavalier",
    "pawn": "pion",
}


@dataclass(frozen=True)
class Report:
    invented_squares: frozenset[str]
    invented_moves: frozenset[str]
    invented_pieces: frozenset[str]
    mentions_best: bool
    names_piece: bool
    sentences: int

    @property
    def faithful(self) -> bool:
        return not (self.invented_squares or self.invented_moves or self.invented_pieces)

    @property
    def invented(self) -> list[str]:
        return sorted(self.invented_squares | self.invented_moves | self.invented_pieces)


def to_french(san: str) -> str:
    """SAN with English piece letters to French (Nd2 -> Cd2)."""
    return _FRENCH_LETTER[san[0]] + san[1:] if san and san[0] in _FRENCH_LETTER else san


def count_sentences(text: str) -> int:
    return sum(1 for part in _SENTENCE_END.split(text) if part.strip())


def _strip(san: str) -> str:
    return san.rstrip("+#")


def check(fact: str, sentence: str) -> Report:
    # In the fact, squares inside moves count too ("best":"Nd2" allows "en d2").
    allowed_squares = set(_ANY_SQUARE.findall(fact))
    best = _BEST.search(fact)
    best_move = best.group(1) if best else None
    allowed_moves = {_strip(best_move), _strip(to_french(best_move))} if best_move else set()
    # Pieces named in the fact, the piece of the best move (Nxd4: a knight), the king of a mate.
    allowed_pieces = {_FRENCH_PIECE[w] for w in _ENGLISH_PIECE.findall(fact)}
    if best_move and best_move[0] in _PIECE_OF_LETTER:
        allowed_pieces.add(_PIECE_OF_LETTER[best_move[0]])
    if "mate" in fact:
        allowed_pieces.add("roi")
    squares = set(_SQUARE.findall(sentence))
    moves = {_strip(m) for m in _MOVE.findall(sentence)}
    pieces = {m.lower() for m in _FRENCH_PIECE_WORD.findall(sentence)}
    piece = _PIECE.search(fact)
    piece_name = _FRENCH_PIECE.get(piece.group(1)) if piece else None
    return Report(
        invented_squares=frozenset(squares - allowed_squares),
        invented_moves=frozenset(moves - allowed_moves),
        invented_pieces=frozenset(pieces - allowed_pieces),
        mentions_best=bool(allowed_moves & moves),
        names_piece=piece_name is not None and piece_name in pieces,
        sentences=count_sentences(sentence),
    )
