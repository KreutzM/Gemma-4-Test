package de.kreutzm.gemma4test.image

import kotlin.math.roundToInt

data class ImageSize(
    val width: Int,
    val height: Int,
) {
    init {
        require(width > 0) { "width must be positive" }
        require(height > 0) { "height must be positive" }
    }
}

data class LetterboxPlan(
    val canvasSize: ImageSize,
    val scaledImageSize: ImageSize,
    val offsetX: Int,
    val offsetY: Int,
)

object ImageResizePlanner {
    fun planScaleToMaxLongEdge(
        source: ImageSize,
        maxLongEdgePx: Int,
    ): ImageSize {
        require(maxLongEdgePx > 0) { "maxLongEdgePx must be positive" }

        val longEdge = maxOf(source.width, source.height)
        if (longEdge <= maxLongEdgePx) return source

        return scaleToLongEdge(source, maxLongEdgePx)
    }

    fun planLetterboxSquare(
        source: ImageSize,
        squareSizePx: Int,
    ): LetterboxPlan {
        require(squareSizePx > 0) { "squareSizePx must be positive" }

        val scaledImageSize = scaleToLongEdge(source, squareSizePx)
        return LetterboxPlan(
            canvasSize = ImageSize(squareSizePx, squareSizePx),
            scaledImageSize = scaledImageSize,
            offsetX = (squareSizePx - scaledImageSize.width) / 2,
            offsetY = (squareSizePx - scaledImageSize.height) / 2,
        )
    }

    private fun scaleToLongEdge(
        source: ImageSize,
        targetLongEdgePx: Int,
    ): ImageSize {
        val longEdge = maxOf(source.width, source.height)
        val scale = targetLongEdgePx.toFloat() / longEdge.toFloat()
        return ImageSize(
            width = (source.width * scale).roundToInt().coerceAtLeast(1),
            height = (source.height * scale).roundToInt().coerceAtLeast(1),
        )
    }
}
