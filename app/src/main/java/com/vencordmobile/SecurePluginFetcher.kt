package com.vencordmobile

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets

/** Bounded HTTPS-only downloader for user-selected plugin sources. */
object SecurePluginFetcher {
    data class Result(val text: String, val finalUrl: String)


    /** Parse a user-selected plugin URL without accepting insecure schemes or URL credentials. */
    internal fun parseHttpsUrl(sourceUrl: String): URL {
        val parsed = URL(sourceUrl)
        require(parsed.protocol.equals("https", ignoreCase = true) && parsed.host.isNotBlank()) {
            "Only valid HTTPS plugin sources are allowed"
        }
        require(parsed.userInfo == null) { "Plugin source URLs must not contain embedded credentials" }
        return parsed
    }

    /** Resolve redirects manually so HTTPS-to-HTTP downgrades can never be followed. */
    internal fun resolveHttpsRedirect(current: URL, location: String): URL {
        require(location.isNotBlank()) { "Redirect did not include a destination" }
        val next = URL(current, location)
        return parseHttpsUrl(next.toString())
    }

    @Throws(Exception::class)
    fun fetch(sourceUrl: String, maxBytes: Int, userAgent: String): Result {
        require(maxBytes > 0) { "Invalid download size limit" }
        require(sourceUrl.length <= 2048) { "Plugin URL is too long" }
        var current = URL(sourceUrl)
        fun validateHttpsUrl(url: URL) {
            require(url.protocol.equals("https", ignoreCase = true)) {
                "Only HTTPS plugin sources are allowed"
            }
            require(!url.host.isNullOrBlank()) { "Plugin URL needs a valid host" }
            require(url.userInfo == null) { "URLs containing embedded credentials are not allowed" }
            require(url.ref == null) { "Plugin URLs must not contain fragments" }
            require(url.port == -1 || url.port in 1..65535) { "Plugin URL has an invalid port" }
        }
        validateHttpsUrl(current)
        for (redirect in 0..5) {
            validateHttpsUrl(current)
            val connection = (current.openConnection() as HttpURLConnection).apply {
                connectTimeout = 15000
                readTimeout = 20000
                useCaches = false
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
                    validateHttpsUrl(next)
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
