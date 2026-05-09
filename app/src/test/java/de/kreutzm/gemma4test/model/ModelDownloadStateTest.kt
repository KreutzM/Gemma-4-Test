package de.kreutzm.gemma4test.model

import org.junit.Assert.assertEquals
import org.junit.Test

class ModelDownloadStateTest {
    @Test
    fun calculatesProgressPercent() {
        val state = ModelDownloadState.Downloading(
            downloadedBytes = 25L,
            totalBytes = 100L,
        )

        assertEquals(25, state.progressPercent)
    }

    @Test
    fun capsProgressPercentAtOneHundred() {
        val state = ModelDownloadState.Downloading(
            downloadedBytes = 125L,
            totalBytes = 100L,
        )

        assertEquals(100, state.progressPercent)
    }

    @Test
    fun zeroTotalBytesReportsZeroProgress() {
        val state = ModelDownloadState.Downloading(
            downloadedBytes = 10L,
            totalBytes = 0L,
        )

        assertEquals(0, state.progressPercent)
    }
}
