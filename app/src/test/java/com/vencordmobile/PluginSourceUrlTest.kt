package com.vencordmobile

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class PluginSourceUrlTest {
    @Test fun rejectsNonHttpsAndMalformedGitHubBlobPaths() {
        listOf("http://github.com/acme/plugin/blob/main/plugin.js",
            "https://github.com/acme/blob/main/plugin.js",
            "https://github.com/acme/plugin/blob/main/").forEach { url ->
            assertThrows(IllegalArgumentException::class.java) { PluginSourceUrl.toRawUrl(url) }
        }
    }

    @Test fun keepsRawGithubFilesAndOrdinaryHttpsJsUrls() {
        val raw = "https://raw.githubusercontent.com/acme/plugin/main/plugin.js"
        assertEquals(raw, PluginSourceUrl.toRawUrl(raw))
        val cdn = "https://cdn.example.org/plugins/a.js"
        assertEquals(cdn, PluginSourceUrl.toRawUrl(cdn))
    }

    @Test fun rejectsGithubLookalikeHostWithBlobPathWhenNotJs() {
        assertThrows(IllegalArgumentException::class.java) {
            PluginSourceUrl.toRawUrl("https://github.com.evil.example/acme/plugin/blob/main/readme.md")
        }
    }

    @Test fun rejectsEmptyOwnerRepositoryOrRef() {
        listOf("https://github.com//plugin/blob/main/a.js",
            "https://github.com/acme//blob/main/a.js",
            "https://github.com/acme/plugin/blob//a.js").forEach { url ->
            assertThrows(IllegalArgumentException::class.java) { PluginSourceUrl.toRawUrl(url) }
        }
    }

    @Test fun rejectsNonstandardGithubPorts() {
        assertThrows(IllegalArgumentException::class.java) {
            PluginSourceUrl.toRawUrl("https://github.com:8443/acme/plugin/blob/main/plugin.js")
        }
        assertEquals("https://raw.githubusercontent.com/acme/plugin/main/plugin.js",
            PluginSourceUrl.toRawUrl("https://github.com:443/acme/plugin/blob/main/plugin.js"))
    }

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
