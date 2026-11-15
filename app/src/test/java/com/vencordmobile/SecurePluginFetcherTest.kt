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
    fun rejectsLoopbackAndPrivateStyleHostnames() {
        listOf(
            "https://localhost/plugin.js",
            "https://sub.localhost/plugin.js",
            "https://router.local/plugin.js",
            "https://nas.lan/plugin.js",
            "https://service.internal/plugin.js",
            "https://machine.test/plugin.js"
        ).forEach { source ->
            assertThrows(IllegalArgumentException::class.java) {
                SecurePluginFetcher.parseHttpsUrl(source)
            }
        }
    }

    @Test
    fun rejectsIpv4Ipv6AndNumericAddressLiterals() {
        listOf(
            "https://127.0.0.1/plugin.js",
            "https://127.1/plugin.js",
            "https://[::1]/plugin.js",
            "https://2130706433/plugin.js",
            "https://0x7f000001/plugin.js"
        ).forEach { source ->
            assertThrows(IllegalArgumentException::class.java) {
                SecurePluginFetcher.parseHttpsUrl(source)
            }
        }
    }

    @Test
    fun rejectsMalformedDnsLabels() {
        listOf("https://-bad.example/plugin.js", "https://bad-.example/plugin.js", "https://bad..example/plugin.js")
            .forEach { source ->
                assertThrows(IllegalArgumentException::class.java) {
                    SecurePluginFetcher.parseHttpsUrl(source)
                }
            }
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
