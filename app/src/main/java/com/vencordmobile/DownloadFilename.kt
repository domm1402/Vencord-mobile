package com.vencordmobile

/** Keep DownloadManager destinations confined to a simple filename under Downloads. */
internal fun safeDownloadFileName(suggested: String): String {
    val leaf = suggested.substringAfterLast('/').substringAfterLast(92.toChar())
    val cleaned = leaf.replace(Regex("[\\u0000-\\u001f<>:\"|?*]"), "_")
        .trim().trim('.').take(180)
    return cleaned.ifBlank { "download" }
}
