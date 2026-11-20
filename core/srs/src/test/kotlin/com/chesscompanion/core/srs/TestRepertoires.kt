package com.chesscompanion.core.srs

import com.chesscompanion.core.chess.Side
import com.chesscompanion.core.chess.pgn.PgnReader
import com.chesscompanion.core.chess.repertoire.Repertoire

/** A repertoire merged from one PGN game per argument (each given headers so that games stay separate). */
internal fun repertoireOf(
    side: Side,
    vararg games: String,
): Repertoire {
    val pgn = games.joinToString("\n\n") { "[Event \"test\"]\n\n$it" }
    return PgnReader.readAll(pgn).games.fold(Repertoire.empty(side)) { r, g -> r.merge(g).repertoire }
}
