package com.vencordmobile

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class PluginSourceUrlTest {
    @Test fun convertsExactGithubBlobHostToRawUrl() {
        assertEquals(
            "https://raw.githubusercontent.com/acme/plugin/main/src/plugin.js",
            PluginSourceUrl.toRawUrl("https://github.com/acme/plugin/blob/main/src/plugin.js")
        )
    }

    @Test fun doesNotTreatLookalikeHostsAsGithub() {
        val url = "https://evilgithub.com/acme/plugin/blob/main/plugin.js"
        assertEquals(url, PluginSourceUrl.toRawUrl(url))
    }

    @Test fun rejectsGithubBlobLinksWithoutJavascriptFiles() {
        assertThrows(IllegalArgumentException::class.java) {
            PluginSourceUrl.toRawUrl("https://github.com/acme/plugin/blob/main/README.md")
        }
    }

    @Test fun rejectsCredentialsAndFragments() {
        listOf("https://user:pass@github.com/acme/plugin/blob/main/plugin.js",
            "https://github.com/acme/plugin/blob/main/plugin.js#section").forEach { url ->
            assertThrows(IllegalArgumentException::class.java) { PluginSourceUrl.toRawUrl(url) }
        }
    }
}
