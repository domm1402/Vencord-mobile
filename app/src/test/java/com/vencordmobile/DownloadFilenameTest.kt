package com.vencordmobile

import org.junit.Assert.assertEquals
import org.junit.Test

class DownloadFilenameTest {
    @Test fun stripsPathTraversalAndKeepsLeaf() {
        assertEquals("report.pdf", safeDownloadFileName("../../report.pdf"))
        assertEquals("image.png", safeDownloadFileName("C:\\temp\\image.png"))
    }

    @Test fun replacesUnsafeCharactersAndTrimsDots() {
        assertEquals("bad_name_.txt", safeDownloadFileName("..bad:name?.txt."))
    }

    @Test fun providesFallbackForEmptyNames() {
        assertEquals("download", safeDownloadFileName("..."))
        assertEquals("download", safeDownloadFileName(""))
    }

    @Test fun removesBidiAndDeleteControls() {
        assertEquals("abcd.txt", safeDownloadFileName("ab\\u202ecd\\u007f.txt"))
    }

    @Test fun removesZeroWidthAndUnicodeFormatCharacters() {
        assertEquals("report.txt", safeDownloadFileName("re\\u200bport\\u00ad.txt"))
        assertEquals("a_b.txt", safeDownloadFileName("a\\u0085b.txt"))
    }

    @Test fun boundsFilenameLength() {
        assertEquals(180, safeDownloadFileName("a".repeat(300)).length)
    }
}
