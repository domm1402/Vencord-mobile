package com.vencordmobile

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PluginManifestValidatorTest {
    @Test fun parsesBoundedValidCatalog() {
        val entries = PluginManifestValidator.parse("""{"plugins":[{"name":"Test","description":"Demo","url":"https://example.com/test.js"}]}""")
        assertEquals(1, entries.size)
        assertEquals("Test", entries[0].name)
        assertEquals("https://example.com/test.js", entries[0].url)
    }

    @Test fun rejectsHttpAndNonJavaScriptSources() {
        assertThrows(IllegalArgumentException::class.java) {
            PluginManifestValidator.parse("""{"plugins":[{"url":"http://example.com/a.js"}]}""")
        }
        assertThrows(IllegalArgumentException::class.java) {
            PluginManifestValidator.parse("""{"plugins":[{"url":"https://example.com/catalog.json"}]}""")
        }
    }

    @Test fun rejectsCredentialsAndFragments() {
        assertThrows(IllegalArgumentException::class.java) {
            PluginManifestValidator.parse("""{"plugins":[{"url":"https://user:pass@example.com/a.js"}]}""")
        }
        assertThrows(IllegalArgumentException::class.java) {
            PluginManifestValidator.parse("""{"plugins":[{"url":"https://example.com/a.js#frag"}]}""")
        }
    }

    @Test fun capsCatalogSizeAndEntryCount() {
        assertThrows(IllegalArgumentException::class.java) {
            PluginManifestValidator.parse(" " + "x".repeat(1_000_000))
        }
        val many = (0..100).joinToString(",") { """{"url":"https://example.com/$it.js"}""" }
        assertThrows(IllegalArgumentException::class.java) {
            PluginManifestValidator.parse("""{"plugins":[$many]}""")
        }
    }

    @Test fun capsDisplayFields() {
        val entry = PluginManifestValidator.parse("""{"plugins":[{"name":"nnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnn","description":"dddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddd","url":"https://example.com/a.js"}]}""")[0]
        assertEquals(80, entry.name.length)
        assertEquals(240, entry.description.length)
    }
}
