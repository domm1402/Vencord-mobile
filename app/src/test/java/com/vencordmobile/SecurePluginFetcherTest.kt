package com.vencordmobile

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.net.URL

class SecurePluginFetcherTest {
    @Test
    fun acceptsHttpsPluginSources() {
        val parsed = SecurePluginFetcher.parseHttpsUrl("https://example.com/plugins/example.js")
        assertEquals("https", parsed.protocol)
        assertEquals("example.com", parsed.host)
    }

    @Test
    fun rejectsPlainHttpPluginSources() {
        assertThrows(IllegalArgumentException::class.java) {
            SecurePluginFetcher.parseHttpsUrl("http://example.com/plugin.js")
        }
    }

    @Test
    fun rejectsEmbeddedUrlCredentials() {
        assertThrows(IllegalArgumentException::class.java) {
            SecurePluginFetcher.parseHttpsUrl("https://user:password@example.com/plugin.js")
        }
    }

    @Test
    fun rejectsUrlsWithFragments() {
        assertThrows(IllegalArgumentException::class.java) {
            SecurePluginFetcher.parseHttpsUrl("https://example.com/plugin.js#section")
        }
    }

    @Test
    fun rejectsOverlongPluginUrls() {
        val url = "https://example.com/" + "a".repeat(2048)
        assertThrows(IllegalArgumentException::class.java) {
            SecurePluginFetcher.parseHttpsUrl(url)
        }
    }

    @Test
    fun resolvesRelativeHttpsRedirects() {
        val source = URL("https://example.com/releases/v1/plugin.js")
        val target = SecurePluginFetcher.resolveHttpsRedirect(source, "../v2/plugin.js")
        assertEquals("https://example.com/releases/v2/plugin.js", target.toString())
    }

    @Test
    fun rejectsHttpsToHttpRedirects() {
        val source = URL("https://example.com/plugin.js")
        assertThrows(IllegalArgumentException::class.java) {
            SecurePluginFetcher.resolveHttpsRedirect(source, "http://example.net/plugin.js")
        }
    }

    @Test
    fun rejectsEmptyRedirectTargets() {
        assertThrows(IllegalArgumentException::class.java) {
            SecurePluginFetcher.resolveHttpsRedirect(URL("https://example.com/plugin.js"), "")
        }
    }
}
