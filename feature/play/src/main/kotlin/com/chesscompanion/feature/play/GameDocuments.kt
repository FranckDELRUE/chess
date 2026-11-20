package com.chesscompanion.feature.play

import android.content.Context
import android.net.Uri
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject

/**
 * Text documents picked by the user through the Storage Access Framework (no storage permission).
 * Moves to `:core:data` when game imports from Lichess and Chess.com arrive (S6).
 */
interface GameDocuments {
    suspend fun read(uri: String): String

    suspend fun write(
        uri: String,
        text: String,
    )
}

class ContentResolverDocuments
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
    ) : GameDocuments {
        override suspend fun read(uri: String): String =
            withContext(Dispatchers.IO) {
                context.contentResolver.openInputStream(Uri.parse(uri))?.use { it.readBytes().toString(Charsets.UTF_8) }
                    ?: throw IOException("Cannot open $uri")
            }

        override suspend fun write(
            uri: String,
            text: String,
        ) = withContext(Dispatchers.IO) {
            context.contentResolver
                .openOutputStream(
                    Uri.parse(uri),
                    "wt",
                )?.use { it.write(text.toByteArray(Charsets.UTF_8)) }
                ?: throw IOException("Cannot write $uri")
        }
    }

@Module
@InstallIn(SingletonComponent::class)
abstract class PlayModule {
    @Binds
    abstract fun bindGameDocuments(impl: ContentResolverDocuments): GameDocuments
}
