package com.vencordmobile

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BackupPreferenceValidatorTest {
    @Test fun acceptsKnownBooleanAndFeatureSettings() {
        assertEquals(true, BackupPreferenceValidator.parse("compact", "boolean", "true"))
        assertEquals(false, BackupPreferenceValidator.parse("feature_hide_gif", "boolean", "false"))
    }

    @Test fun acceptsKnownIntegerValuesWithinRanges() {
        assertEquals(10, BackupPreferenceValidator.parse("theme", "int", "10"))
        assertEquals(5, BackupPreferenceValidator.parse("font_choice", "int", "5"))
        assertEquals(5, BackupPreferenceValidator.parse("rpc_type", "int", "5"))
        assertEquals(175, BackupPreferenceValidator.parse("page_text_zoom", "int", "175"))
    }

    @Test fun rejectsUnknownKeysAndExecutablePlugins() {
        assertNull(BackupPreferenceValidator.parse("some_arbitrary_key", "string", "value"))
        assertNull(BackupPreferenceValidator.parse("custom_plugins", "string", "[]"))
    }

    @Test fun rejectsMismatchedTypesAndMalformedBooleans() {
        assertNull(BackupPreferenceValidator.parse("compact", "string", "true"))
        assertNull(BackupPreferenceValidator.parse("compact", "boolean", "yes"))
        assertNull(BackupPreferenceValidator.parse("theme", "boolean", "false"))
    }

    @Test fun rejectsOutOfRangeOrMalformedIntegers() {
        assertNull(BackupPreferenceValidator.parse("theme", "int", "999"))
        assertNull(BackupPreferenceValidator.parse("font_choice", "int", "-1"))
        assertNull(BackupPreferenceValidator.parse("rpc_type", "int", "not-a-number"))
        assertNull(BackupPreferenceValidator.parse("page_text_zoom", "int", "201"))
    }

    @Test fun acceptsStringAtTheExactSizeLimit() {
        val exactLimit = "x".repeat(100000)
        assertEquals(exactLimit, BackupPreferenceValidator.parse("css", "string", exactLimit))
        assertEquals(exactLimit, BackupPreferenceValidator.parse("rpc_game", "string", exactLimit))
    }

    @Test fun rejectsOversizedRpcStringsAndWrongRpcTypes() {
        assertNull(BackupPreferenceValidator.parse("rpc_game", "string", "x".repeat(100001)))
        assertNull(BackupPreferenceValidator.parse("rpc_game", "int", "1"))
        assertNull(BackupPreferenceValidator.parse("rpc_game", "boolean", "true"))
    }

    @Test fun boundsRestoredText() {
        assertNull(BackupPreferenceValidator.parse("css", "string", "x".repeat(100001)))
        assertEquals("safe", BackupPreferenceValidator.parse("css", "string", "safe"))
    }
}
