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

    @Test fun boundsFilenameLength() {
        assertEquals(180, safeDownloadFileName("a".repeat(300)).length)
    }
}
