package com.chesscompanion.feature.play

import java.io.IOException

/** In-memory documents: [files] maps a URI to its text; an unknown URI fails like an unreadable file. */
class FakeDocuments(
    val files: MutableMap<String, String> = mutableMapOf(),
) : GameDocuments {
    override suspend fun read(uri: String): String = files[uri] ?: throw IOException("No such document $uri")

    override suspend fun write(
        uri: String,
        text: String,
    ) {
        if (uri.startsWith("readonly:")) throw IOException("Read-only $uri")
        files[uri] = text
    }
}
