package com.chesscompanion.feature.repertoire

import com.chesscompanion.core.chess.Side

fun repertoireRowTag(id: String) = "repertoire-row-$id"

fun repertoireRenameTag(id: String) = "repertoire-rename-$id"

fun repertoireDeleteTag(id: String) = "repertoire-delete-$id"

fun repertoireDrillTag(id: String) = "repertoire-drill-$id"

fun repertoireSideTag(side: Side) = "repertoire-side-${side.name.lowercase()}"
