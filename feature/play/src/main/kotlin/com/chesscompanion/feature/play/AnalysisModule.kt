package com.chesscompanion.feature.play

import com.chesscompanion.core.engine.EnginePriority
import com.chesscompanion.core.engine.EngineScheduler
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** The live analysis shares the single Stockfish through the scheduler, below the opponent (ADR 0008). */
@Module
@InstallIn(SingletonComponent::class)
object AnalysisModule {
    @Provides
    fun providePositionAnalyzer(scheduler: EngineScheduler): PositionAnalyzer =
        PositionAnalyzer { scheduler.search(EnginePriority.LIVE, it) }
}
