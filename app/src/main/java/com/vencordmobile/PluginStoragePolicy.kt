package com.vencordmobile

import org.json.JSONArray

/** Keep local plugin storage bounded before it is persisted or injected into WebView. */
internal object PluginStoragePolicy {
    const val MAX_PLUGINS = 50
    const val MAX_PLUGIN_BYTES = 500_000
    const val MAX_TOTAL_CODE_BYTES = 2_000_000

    fun validate(plugins: JSONArray): String? {
        if (plugins.length() > MAX_PLUGINS) return "You can store up to $MAX_PLUGINS plugins"
        var total = 0
        for (index in 0 until plugins.length()) {
            val plugin = plugins.optJSONObject(index) ?: return "Plugin entry $index is invalid"
            // optString coerces numbers/objects into strings. Require actual source text
            // so malformed imports cannot turn arbitrary JSON values into executable input.
            val rawName = plugin.opt("name")
            if (rawName != null && rawName !is String) return "Plugin entry $index has invalid name"
            if (((rawName as? String)?.length ?: 0) > 80) return "Plugin entry $index name exceeds 80 characters"
            val rawSource = plugin.opt("source")
            if (rawSource != null && rawSource !is String) return "Plugin entry $index has invalid source URL"
            if (((rawSource as? String)?.length ?: 0) > 2048) return "Plugin entry $index source URL is too long"
            val rawEnabled = plugin.opt("enabled")
            if (rawEnabled != null && rawEnabled !is Boolean) return "Plugin entry $index has invalid enabled flag"
            val rawCode = plugin.opt("code")
            if (rawCode !is String) return "Plugin entry $index has invalid code"
            val code = rawCode
            if (code.isBlank()) return "Plugin entry $index has empty code"
            val size = code.toByteArray(Charsets.UTF_8).size
            if (size > MAX_PLUGIN_BYTES) return "A plugin exceeds the 500 KB limit"
            // Check before addition to avoid integer overflow if limits are ever raised.
            if (size > MAX_TOTAL_CODE_BYTES - total) return "Combined plugin code exceeds 2 MB"
            total += size
        }
        return null
    }
}
