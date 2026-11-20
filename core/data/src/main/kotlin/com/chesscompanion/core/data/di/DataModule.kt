package com.chesscompanion.core.data.di

import android.content.Context
import androidx.room.Room
import com.chesscompanion.core.data.AnalysisStore
import com.chesscompanion.core.data.ChessDatabase
import com.chesscompanion.core.data.Migrations
import com.chesscompanion.core.data.RepertoireStore
import com.chesscompanion.core.data.ReviewCardStore
import com.chesscompanion.core.data.RoomAnalysisStore
import com.chesscompanion.core.data.RoomRepertoireStore
import com.chesscompanion.core.data.RoomReviewCardStore
import com.chesscompanion.core.data.explorer.CachedExplorer
import com.chesscompanion.core.data.explorer.ExplorerLookup
import com.chesscompanion.core.data.explorer.GapFinder
import com.chesscompanion.core.data.explorer.KeystoreTokenStore
import com.chesscompanion.core.data.explorer.KtorExplorerFetcher
import com.chesscompanion.core.data.explorer.TokenStore
import com.chesscompanion.core.data.study.KtorStudyFetcher
import com.chesscompanion.core.data.study.StudyFetcher
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import java.time.Clock
import javax.inject.Singleton

private const val CONNECT_TIMEOUT_MS = 10_000L
private const val REQUEST_TIMEOUT_MS = 30_000L

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
            .addMigrations(Migrations.V1_TO_V2, Migrations.V2_TO_V3, Migrations.V3_TO_V4)
            .build()

    @Provides
    @Singleton
    fun provideAnalysisStore(database: ChessDatabase): AnalysisStore = RoomAnalysisStore(database)

    @Provides
    @Singleton
    fun provideRepertoireStore(database: ChessDatabase): RepertoireStore = RoomRepertoireStore(database)

    @Provides
    @Singleton
    fun provideClock(): Clock = Clock.systemDefaultZone()

    @Provides
    @Singleton
    fun provideReviewCardStore(database: ChessDatabase): ReviewCardStore = RoomReviewCardStore(database)

    @Provides
    @Singleton
    fun provideHttpClient(): HttpClient =
        HttpClient(OkHttp) {
            expectSuccess = false
            install(HttpTimeout) {
                connectTimeoutMillis = CONNECT_TIMEOUT_MS
                requestTimeoutMillis = REQUEST_TIMEOUT_MS
            }
            // A generic client name: no account, no contact detail. No token here (ADR 0013).
            defaultRequest { header(HttpHeaders.UserAgent, "ChessCompanion") }
        }

    @Provides
    @Singleton
    fun provideStudyFetcher(client: HttpClient): StudyFetcher = KtorStudyFetcher(client)

    @Provides
    @Singleton
    fun provideTokenStore(
        @ApplicationContext context: Context,
    ): TokenStore = KeystoreTokenStore(context)

    @Provides
    @Singleton
    fun provideGapFinder(explorer: ExplorerLookup): GapFinder = GapFinder(explorer)

    @Provides
    @Singleton
    fun provideExplorer(
        client: HttpClient,
        tokens: TokenStore,
        database: ChessDatabase,
    ): ExplorerLookup = CachedExplorer(KtorExplorerFetcher(client, tokens), database.explorerDao())
}
