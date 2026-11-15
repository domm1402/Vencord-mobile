package com.vencordmobile

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets

/** Bounded HTTPS-only downloader for user-selected plugin sources. */
object SecurePluginFetcher {
    data class Result(val text: String, val finalUrl: String)

    @Throws(Exception::class)
    fun fetch(sourceUrl: String, maxBytes: Int, userAgent: String): Result {
        require(maxBytes > 0) { "Invalid download size limit" }
        var current = URL(sourceUrl)
        require(current.protocol.equals("https", ignoreCase = true)) {
            "Only HTTPS plugin sources are allowed"
        }
        for (redirect in 0..5) {
            require(current.protocol.equals("https", ignoreCase = true)) {
                "Redirected plugin source must remain HTTPS"
            }
            val connection = (current.openConnection() as HttpURLConnection).apply {
                connectTimeout = 15000
                readTimeout = 20000
                instanceFollowRedirects = false
                requestMethod = "GET"
                setRequestProperty("User-Agent", userAgent)
                setRequestProperty("Accept", "application/javascript, application/json, text/plain, */*")
            }
            try {
                val status = connection.responseCode
                if (status in 300..399) {
                    val location = connection.getHeaderField("Location")
                        ?: throw IOException("Redirect did not include a destination")
                    if (redirect == 5) throw IOException("Too many redirects")
                    val next = URL(current, location)
                    require(next.protocol.equals("https", ignoreCase = true)) {
                        "Blocked insecure HTTP redirect"
                    }
                    current = next
                    continue
                }
                if (status !in 200..299) throw IOException("HTTP " + status)
                val declaredLength = connection.contentLengthLong
                if (declaredLength > maxBytes) throw IOException("File exceeds " + (maxBytes / 1000) + " KB limit")
                val bytes = ByteArrayOutputStream()
                connection.inputStream.use { input ->
                    val buffer = ByteArray(8192)
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        if (bytes.size() + count > maxBytes) {
                            throw IOException("File exceeds " + (maxBytes / 1000) + " KB limit")
                        }
                        bytes.write(buffer, 0, count)
                    }
                }
                return Result(String(bytes.toByteArray(), StandardCharsets.UTF_8), current.toString())
            } finally {
                connection.disconnect()
            }
        }
        throw IOException("Could not download plugin source")
    }
}
