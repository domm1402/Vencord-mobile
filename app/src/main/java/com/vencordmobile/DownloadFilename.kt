package com.vencordmobile

/** Keep DownloadManager destinations confined to a simple filename under Downloads. */
internal fun safeDownloadFileName(suggested: String): String {
    val leaf = suggested.substringAfterLast('/').substringAfterLast(92.toChar())
    // Strip invisible Unicode formatting and control characters to prevent spoofing.
    val visible = leaf.filterNot {
        Character.getType(it).toInt() == Character.FORMAT.toInt() ||
            it.code in 0..31 || it.code in 127..159 ||
            it in '\u202a'..'\u202e' || it in '\u2066'..'\u2069'
    }
    // Replace filesystem-reserved characters while preserving a readable filename.
    val cleaned = visible.map { ch -> if (ch in "<>:\"|?*") '_' else ch }
        .joinToString("").trim().trimEnd().trim('.').take(180)
    return cleaned.ifBlank { "download" }
}
