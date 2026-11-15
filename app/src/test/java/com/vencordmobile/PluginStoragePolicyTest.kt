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
        repeat(PluginStoragePolicy.MAX_PLUGINS + 1) { plugins.put(JSONObject().put("code","console.log(1)")) }
        assertEquals("You can store up to 50 plugins", PluginStoragePolicy.validate(plugins))
    }

    @Test fun capsIndividualAndCombinedCode() {
        assertEquals("A plugin exceeds the 500 KB limit",
            PluginStoragePolicy.validate(JSONArray().put(JSONObject().put("code","x".repeat(500_001)))))
        val plugins = JSONArray()
        repeat(5) { plugins.put(JSONObject().put("code", "x".repeat(500_000))) }
        assertEquals("Combined plugin code exceeds 2 MB", PluginStoragePolicy.validate(plugins))
    }

    @Test fun rejectsMalformedEntriesAndNonStringCode() {
        assertEquals("Plugin entry 0 is invalid", PluginStoragePolicy.validate(JSONArray().put("not an object")))
        assertEquals("Plugin entry 0 has invalid code", PluginStoragePolicy.validate(JSONArray().put(JSONObject().put("code", 123))))
        assertEquals("Plugin entry 0 has invalid code", PluginStoragePolicy.validate(JSONArray().put(JSONObject().put("code", JSONObject()))))
    }

    @Test fun rejectsMissingAndEmptyPluginSource() {
        assertEquals("Plugin entry 0 has invalid code", PluginStoragePolicy.validate(JSONArray().put(JSONObject())))
        assertEquals("Plugin entry 0 has empty code", PluginStoragePolicy.validate(JSONArray().put(JSONObject().put("code", "   "))))
    }

    @Test fun countsUtf8BytesRatherThanCharacters() {
        val code = "é".repeat(250_001) // 500,002 UTF-8 bytes
        assertEquals("A plugin exceeds the 500 KB limit",
            PluginStoragePolicy.validate(JSONArray().put(JSONObject().put("code", code))))
    }
}
