package com.chesscompanion.core.data.study

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class StudyRefTest {
    @Test
    fun `a bare identifier is a study`() {
        assertEquals(StudyRef("Y1yXP80U"), StudyRef.parse("  Y1yXP80U "))
    }

    @Test
    fun `an address gives the study, and the chapter when there is one`() {
        assertEquals(StudyRef("Y1yXP80U"), StudyRef.parse("https://lichess.org/study/Y1yXP80U"))
        assertEquals(StudyRef("Y1yXP80U", "oxgS1aRW"), StudyRef.parse("https://lichess.org/study/Y1yXP80U/oxgS1aRW"))
        assertEquals(StudyRef("Y1yXP80U"), StudyRef.parse("lichess.org/study/Y1yXP80U/"))
        assertEquals(StudyRef("Y1yXP80U"), StudyRef.parse("https://www.lichess.org/study/Y1yXP80U.pgn"))
        assertEquals(
            StudyRef("Y1yXP80U", "oxgS1aRW"),
            StudyRef.parse("https://lichess.org/study/embed/Y1yXP80U/oxgS1aRW"),
        )
        assertEquals(StudyRef("Y1yXP80U"), StudyRef.parse("https://lichess.org/study/Y1yXP80U?color=white#12"))
    }

    @Test
    fun `anything else is refused, other hosts included`() {
        listOf(
            "",
            "Y1yXP80",
            "Y1yXP80UU",
            "https://example.com/study/Y1yXP80U",
            "https://lichess.org.evil.com/study/Y1yXP80U",
            "https://lichess.org/@/someone",
            "https://lichess.org/study/Y1yXP80U/short",
            "http://localhost/study/Y1yXP80U",
            "Y1yXP8 0U",
        ).forEach { assertNull(StudyRef.parse(it), it) }
    }

    @Test
    fun `the path is built from the identifiers`() {
        assertEquals("/api/study/Y1yXP80U.pgn", StudyRef("Y1yXP80U").pgnPath)
        assertEquals("/api/study/Y1yXP80U/oxgS1aRW.pgn", StudyRef("Y1yXP80U", "oxgS1aRW").pgnPath)
    }
}
