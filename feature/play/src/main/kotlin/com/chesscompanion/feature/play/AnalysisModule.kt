package com.chesscompanion.feature.play

import com.chesscompanion.core.data.AnalysisStore
import com.chesscompanion.core.engine.EnginePriority
import com.chesscompanion.core.engine.EngineScheduler
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** The live analysis and the opponent share the single Stockfish through the scheduler (ADR 0008). */
@Module
@InstallIn(SingletonComponent::class)
object AnalysisModule {
    @Provides
    fun providePositionAnalyzer(scheduler: EngineScheduler): PositionAnalyzer =
        PositionAnalyzer { scheduler.search(EnginePriority.LIVE, it) }

    /** The background analysis gives way to everything else (ADR 0008). */
    @Provides
    fun provideBackgroundEngine(scheduler: EngineScheduler): BackgroundEngine =
        BackgroundEngine { scheduler.search(EnginePriority.BACKGROUND, it) }

    @Provides
    fun provideGameAnalyzer(
        engine: BackgroundEngine,
        store: AnalysisStore,
    ): GameAnalyzer = GameAnalyzer(engine, store)

    @Provides
    fun provideGameAnalysisQueue(queue: WorkManagerAnalysisQueue): GameAnalysisQueue = queue

    /** Stockfish's moves against the player come first: they pre-empt the analysis (ADR 0008). */
    @Provides
    fun provideOpponentEngine(scheduler: EngineScheduler): OpponentEngine =
        OpponentEngine { scheduler.search(EnginePriority.OPPONENT, it) }
}
