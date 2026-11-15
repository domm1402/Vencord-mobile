package com.vencordmobile

import java.net.URL
import java.util.Locale

/** Canonicalize supported GitHub blob URLs without trusting substring host matches. */
internal object PluginSourceUrl {
    fun toRawUrl(input: String): String {
        require(input.length <= 2048) { "Plugin URL is too long" }
        require(input.startsWith("https://", ignoreCase = true)) { "Only HTTPS URLs are allowed" }
        val parsed = URL(input)
        require(parsed.protocol.equals("https", ignoreCase = true)) { "Only HTTPS URLs are allowed" }
        require(parsed.userInfo == null && parsed.ref == null) { "Plugin URLs cannot contain credentials or fragments" }
        val host = parsed.host.lowercase(Locale.ROOT).removeSuffix(".")
        if (host == "github.com" || host == "raw.githubusercontent.com") {
            require(parsed.port == -1 || parsed.port == 443) { "GitHub plugin URLs must use the standard HTTPS port" }
        }
        if (host == "github.com" && parsed.path.contains("/blob/")) {
            val parts = parsed.path.removePrefix("/").split("/")
            require(parts.size >= 5 && parts[0].isNotBlank() && parts[1].isNotBlank() && parts[2] == "blob" && parts[3].isNotBlank()) { "Use a GitHub link to a specific .js file" }
            require(parts.last().endsWith(".js", ignoreCase = true)) { "GitHub plugin links must point to a .js file" }
            return "https://raw.githubusercontent.com/${parts[0]}/${parts[1]}/${parts[3]}/" + parts.drop(4).joinToString("/")
        }
        require(host == "raw.githubusercontent.com" || parsed.path.endsWith(".js", ignoreCase = true)) {
            "Use a direct .js URL or GitHub file link"
        }
        return input
    }
}
