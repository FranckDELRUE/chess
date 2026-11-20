package com.chesscompanion.feature.repertoire

import com.chesscompanion.core.data.study.StudyFetcher
import com.chesscompanion.core.data.study.StudyRef
import com.chesscompanion.core.data.study.StudyResult

/** [StudyFetcher] that answers [result] and remembers what was asked. */
class FakeStudyFetcher(
    var result: StudyResult = StudyResult.Offline,
) : StudyFetcher {
    val asked = mutableListOf<StudyRef>()

    override suspend fun fetch(ref: StudyRef): StudyResult {
        asked += ref
        return result
    }
}
