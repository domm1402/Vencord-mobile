package com.vencordmobile

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PluginStoragePolicyTest {
    @Test fun acceptsSmallPluginCollection() {
        assertNull(PluginStoragePolicy.validate(JSONArray().put(JSONObject().put("code","console.log(1)"))))
    }

    @Test fun capsPluginCount() {
        val plugins = JSONArray()
        repeat(PluginStoragePolicy.MAX_PLUGINS + 1) { plugins.put(JSONObject().put("code","")) }
        assertEquals("You can store up to 50 plugins", PluginStoragePolicy.validate(plugins))
    }

    @Test fun capsIndividualAndCombinedCode() {
        assertEquals("A plugin exceeds the 500 KB limit",
            PluginStoragePolicy.validate(JSONArray().put(JSONObject().put("code","x".repeat(500_001)))))
        val plugins = JSONArray()
        repeat(5) { plugins.put(JSONObject().put("code", "x".repeat(500_000))) }
        assertEquals("Combined plugin code exceeds 2 MB", PluginStoragePolicy.validate(plugins))
    }

    @Test fun rejectsMalformedEntries() {
        assertEquals("Plugin entry 0 is invalid", PluginStoragePolicy.validate(JSONArray().put("not an object")))
    }
}
