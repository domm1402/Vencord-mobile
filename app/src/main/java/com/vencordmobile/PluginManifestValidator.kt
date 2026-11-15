package com.vencordmobile

import org.json.JSONObject
import java.net.URL

/** Bounds and validates untrusted plugin catalog JSON before it reaches UI or installer. */
internal object PluginManifestValidator {
    data class Entry(val name: String, val description: String, val url: String)

    fun parse(json: String): List<Entry> {
        require(json.toByteArray(Charsets.UTF_8).size <= 1_000_000) { "Manifest exceeds 1 MB" }
        val plugins = JSONObject(json).optJSONArray("plugins")
            ?: throw IllegalArgumentException("Manifest needs a plugins array")
        require(plugins.length() <= 100) { "Manifest may contain at most 100 plugins" }
        return (0 until plugins.length()).map { index ->
            val item = plugins.optJSONObject(index)
                ?: throw IllegalArgumentException("Plugin entry $index is not an object")
            val name = item.optString("name", "Unnamed plugin").trim().take(80).ifBlank { "Unnamed plugin" }
            val description = item.optString("description", "No description").trim().take(240)
            val source = item.optString("url", "").trim()
            val parsed = SecurePluginFetcher.parseHttpsUrl(source)
            require(parsed.path.endsWith(".js", ignoreCase = true)) { "Plugin $name must link to a .js file" }
            Entry(name, description, source)
        }
    }
}
