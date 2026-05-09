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

object ImageResizePlanner {
    fun planScaleToMaxLongEdge(
        source: ImageSize,
        maxLongEdgePx: Int,
    ): ImageSize {
        require(maxLongEdgePx > 0) { "maxLongEdgePx must be positive" }

        val longEdge = maxOf(source.width, source.height)
        if (longEdge <= maxLongEdgePx) return source

        val scale = maxLongEdgePx.toFloat() / longEdge.toFloat()
        return ImageSize(
            width = (source.width * scale).roundToInt().coerceAtLeast(1),
            height = (source.height * scale).roundToInt().coerceAtLeast(1),
        )
    }
}
