package com.vencordmobile

/** Keep DownloadManager destinations confined to a simple filename under Downloads. */
internal fun safeDownloadFileName(suggested: String): String {
    val leaf = suggested.substringAfterLast('/').substringAfterLast(92.toChar())
    // Unicode FORMAT characters include zero-width spaces and directional controls.
    // Remove them rather than allowing invisible spoofing in a Downloads listing.
    val visible = leaf.filterNot {
        Character.getType(it) == Character.FORMAT.toInt() ||
            it in '\u202a'..'\u202e' || it in '\u2066'..'\u2069'
    }
    val cleaned = visible.replace(Regex("[\\u0000-\\u001f\\u007f-\\u009f<>:\\"|?*]"), "_")
        .trim().trimEnd().trim('.').take(180)
    return cleaned.ifBlank { "download" }
}
