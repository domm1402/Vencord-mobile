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
            val code = plugin.optString("code", "")
            val size = code.toByteArray(Charsets.UTF_8).size
            if (size > MAX_PLUGIN_BYTES) return "A plugin exceeds the 500 KB limit"
            total += size
            if (total > MAX_TOTAL_CODE_BYTES) return "Combined plugin code exceeds 2 MB"
        }
        return null
    }
}
