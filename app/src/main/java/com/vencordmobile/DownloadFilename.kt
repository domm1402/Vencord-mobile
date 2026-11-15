package com.vencordmobile

/** Keep DownloadManager destinations confined to a simple filename under Downloads. */
internal fun safeDownloadFileName(suggested: String): String {
    val leaf = suggested.substringAfterLast('/').substringAfterLast(92.toChar())
    // Strip invisible Unicode formatting controls to prevent misleading filenames.
    val visible = leaf.filterNot {
        Character.getType(it) == Character.FORMAT ||
            it in '\u202a'..'\u202e' || it in '\u2066'..'\u2069'
    }
    val cleaned = visible.map { ch ->
        if (ch.code in 0..31 || ch.code in 127..159 ||
            ch in listOf('<', '>', ':', '"', '|', '?', '*')) '_' else ch
    }.joinToString("").trim().trimEnd().trim('.').take(180)
    return cleaned.ifBlank { "download" }
}
