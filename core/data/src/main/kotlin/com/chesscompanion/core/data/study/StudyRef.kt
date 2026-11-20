package com.chesscompanion.core.data.study

/**
 * A public Lichess study, or one chapter of it. Only the identifiers are kept: the address that is
 * requested is always built from them, never taken from what the user typed.
 */
data class StudyRef(
    val studyId: String,
    val chapterId: String? = null,
) {
    /** Path of the PGN of the study (all chapters) or of the chapter, on the Lichess API. */
    val pgnPath: String
        get() = if (chapterId == null) "/api/study/$studyId.pgn" else "/api/study/$studyId/$chapterId.pgn"

    companion object {
        private const val ID_LENGTH = 8
        private val id = Regex("[A-Za-z0-9]{$ID_LENGTH}")
        private val address =
            Regex(
                """^(?:https?://)?(?:www\.)?lichess\.org/study/(?:embed/)?([A-Za-z0-9]{8})""" +
                    """(?:/([A-Za-z0-9]{8}))?(?:\.pgn)?/?(?:[?#].*)?$""",
            )

        /** The study named by [input] (an identifier or a lichess.org study address), null otherwise. */
        fun parse(input: String): StudyRef? {
            val text = input.trim()
            if (id.matches(text)) return StudyRef(text)
            val match = address.matchEntire(text) ?: return null
            return StudyRef(match.groupValues[1], match.groupValues[2].ifEmpty { null })
        }
    }
}
