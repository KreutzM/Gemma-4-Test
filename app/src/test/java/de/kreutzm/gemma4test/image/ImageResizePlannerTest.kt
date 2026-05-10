package de.kreutzm.gemma4test.image

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class ImageResizePlannerTest {
    @Test
    fun keepsSmallImageUnchanged() {
        val result = ImageResizePlanner.planScaleToMaxLongEdge(
            source = ImageSize(width = 800, height = 600),
            maxLongEdgePx = 1024,
        )

        assertEquals(ImageSize(width = 800, height = 600), result)
    }

    @Test
    fun scalesLandscapeImageToMaxLongEdge() {
        val result = ImageResizePlanner.planScaleToMaxLongEdge(
            source = ImageSize(width = 4000, height = 3000),
            maxLongEdgePx = 1000,
        )

        assertEquals(ImageSize(width = 1000, height = 750), result)
    }

    @Test
    fun scalesPortraitImageToMaxLongEdge() {
        val result = ImageResizePlanner.planScaleToMaxLongEdge(
            source = ImageSize(width = 3000, height = 4000),
            maxLongEdgePx = 1000,
        )

        assertEquals(ImageSize(width = 750, height = 1000), result)
    }

    @Test
    fun letterboxesTallPreviewIntoSquareCanvas() {
        val result = ImageResizePlanner.planLetterboxSquare(
            source = ImageSize(width = 141, height = 250),
            squareSizePx = 512,
        )

        assertEquals(ImageSize(width = 512, height = 512), result.canvasSize)
        assertEquals(ImageSize(width = 289, height = 512), result.scaledImageSize)
        assertEquals(111, result.offsetX)
        assertEquals(0, result.offsetY)
    }

    @Test
    fun letterboxesWidePreviewIntoSquareCanvas() {
        val result = ImageResizePlanner.planLetterboxSquare(
            source = ImageSize(width = 250, height = 141),
            squareSizePx = 512,
        )

        assertEquals(ImageSize(width = 512, height = 512), result.canvasSize)
        assertEquals(ImageSize(width = 512, height = 289), result.scaledImageSize)
        assertEquals(0, result.offsetX)
        assertEquals(111, result.offsetY)
    }

    @Test
    fun rejectsInvalidSourceSize() {
        assertThrows(IllegalArgumentException::class.java) {
            ImageSize(width = 0, height = 100)
        }
    }

    @Test
    fun rejectsInvalidMaxLongEdge() {
        assertThrows(IllegalArgumentException::class.java) {
            ImageResizePlanner.planScaleToMaxLongEdge(
                source = ImageSize(width = 100, height = 100),
                maxLongEdgePx = 0,
            )
        }
    }

    @Test
    fun rejectsInvalidLetterboxSize() {
        assertThrows(IllegalArgumentException::class.java) {
            ImageResizePlanner.planLetterboxSquare(
                source = ImageSize(width = 100, height = 100),
                squareSizePx = 0,
            )
        }
    }
}
