package com.chesscompanion.core.engine.di

import android.content.Context
import com.chesscompanion.core.engine.EngineScheduler
import com.chesscompanion.core.engine.StockfishEngine
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Stockfish allows one session per process: a single scheduler owns it for the whole app (ADR 0008). */
@Module
@InstallIn(SingletonComponent::class)
object EngineModule {
    @Provides
    @Singleton
    fun provideEngineScheduler(
        @ApplicationContext context: Context,
    ): EngineScheduler = EngineScheduler { StockfishEngine.start(context) }
}
