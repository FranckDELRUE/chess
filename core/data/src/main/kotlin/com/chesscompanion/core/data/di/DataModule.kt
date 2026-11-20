package com.chesscompanion.core.data.di

import android.content.Context
import androidx.room.Room
import com.chesscompanion.core.data.AnalysisStore
import com.chesscompanion.core.data.ChessDatabase
import com.chesscompanion.core.data.Migrations
import com.chesscompanion.core.data.RepertoireStore
import com.chesscompanion.core.data.RoomAnalysisStore
import com.chesscompanion.core.data.RoomRepertoireStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {
    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
    ): ChessDatabase =
        Room
            .databaseBuilder(context, ChessDatabase::class.java, ChessDatabase.NAME)
            .addMigrations(Migrations.V1_TO_V2)
            .build()

    @Provides
    @Singleton
    fun provideAnalysisStore(database: ChessDatabase): AnalysisStore = RoomAnalysisStore(database)

    @Provides
    @Singleton
    fun provideRepertoireStore(database: ChessDatabase): RepertoireStore = RoomRepertoireStore(database)
}
