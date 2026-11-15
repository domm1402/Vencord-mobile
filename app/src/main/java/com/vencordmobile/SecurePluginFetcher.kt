package com.vencordmobile

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets

/** Bounded HTTPS-only downloader for user-selected plugin sources. */
object SecurePluginFetcher {
    data class Result(val text: String, val finalUrl: String)


    /** Validate plugin source URLs before opening a network connection. */
    internal fun validateHttpsUrl(url: URL) {
        require(url.protocol.equals("https", ignoreCase = true)) {
            "Only HTTPS plugin sources are allowed"
        }
        val host = url.host.lowercase(java.util.Locale.ROOT).removeSuffix(".")
        require(host.length in 1..253 && host.contains(".")) { "Plugin URL needs a public DNS hostname" }
        require(host.split(".").all { label ->
            label.length in 1..63 && label.matches(Regex("[a-z0-9](?:[a-z0-9-]*[a-z0-9])?"))
        }) { "Plugin URL contains an invalid hostname" }
        // Never fetch from loopback, private-style local names, or IP-literal hosts.
        // This prevents obvious local-network/localhost targets; DNS rebinding is
        // still outside this URL-only check and is not claimed to be prevented.
        require(!host.contains(":") && !host.matches(Regex("[0-9.]+")) &&
            !host.matches(Regex("(?i)(0x[0-9a-f]+|[0-9]+)"))) {
            "Plugin sources must use a DNS hostname, not an IP address"
        }
        val localSuffixes = setOf("localhost", "local", "localdomain", "lan", "home", "internal", "test", "invalid")
        require(host != "localhost" && localSuffixes.none { host == it || host.endsWith(".$it") }) {
            "Local or reserved hostnames are not allowed for plugin sources"
        }
        require(url.userInfo == null) { "URLs containing embedded credentials are not allowed" }
        require(url.ref == null) { "Plugin URLs must not contain fragments" }
        require(url.port == -1 || url.port in 1..65535) { "Plugin URL has an invalid port" }
    }

    internal fun parseHttpsUrl(sourceUrl: String): URL {
        require(sourceUrl.length <= 2048) { "Plugin URL is too long" }
        return URL(sourceUrl).also(::validateHttpsUrl)
    }

    /** Resolve redirects manually so HTTPS-to-HTTP downgrades can never be followed. */
    internal fun resolveHttpsRedirect(current: URL, location: String): URL {
        require(location.isNotBlank()) { "Redirect did not include a destination" }
        return URL(current, location).also(::validateHttpsUrl)
    }

    @Throws(Exception::class)
    fun fetch(sourceUrl: String, maxBytes: Int, userAgent: String): Result {
        require(maxBytes in 1..5_000_000) { "Download size limit must be between 1 byte and 5 MB" }
        var current = parseHttpsUrl(sourceUrl)
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
                    current = resolveHttpsRedirect(current, location)
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
