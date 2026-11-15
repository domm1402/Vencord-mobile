package com.vencordmobile

/** Keep DownloadManager destinations confined to a simple filename under Downloads. */
internal fun safeDownloadFileName(suggested: String): String {
    val leaf = suggested.substringAfterLast('/').substringAfterLast(92.toChar())
    val visible = leaf.filterNot {
        it == '\u007f' || it in '\u202a'..'\u202e' || it in '\u2066'..'\u2069'
    }
    val cleaned = visible.replace(Regex("[\\u0000-\\u001f<>:\\"|?*]"), "_")
        .trim().trimEnd().trim('.').take(180)
    return cleaned.ifBlank { "download" }
}
