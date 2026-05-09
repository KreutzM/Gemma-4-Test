package de.kreutzm.gemma4test.image

import android.graphics.Bitmap
import java.io.ByteArrayOutputStream
import kotlin.math.roundToInt

object ImagePreprocessor {
    const val DEFAULT_MAX_LONG_EDGE_PX = 1024

    fun scaleToMaxLongEdge(
        source: Bitmap,
        maxLongEdgePx: Int = DEFAULT_MAX_LONG_EDGE_PX,
    ): Bitmap {
        require(maxLongEdgePx > 0) { "maxLongEdgePx must be positive" }

        val width = source.width
        val height = source.height
        require(width > 0 && height > 0) { "Bitmap dimensions must be positive" }

        val longEdge = maxOf(width, height)
        if (longEdge <= maxLongEdgePx) return source

        val scale = maxLongEdgePx.toFloat() / longEdge.toFloat()
        val targetWidth = (width * scale).roundToInt().coerceAtLeast(1)
        val targetHeight = (height * scale).roundToInt().coerceAtLeast(1)

        return Bitmap.createScaledBitmap(source, targetWidth, targetHeight, true)
    }

    fun toPngBytes(bitmap: Bitmap): ByteArray {
        val output = ByteArrayOutputStream()
        check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)) { "Could not encode bitmap as PNG" }
        return output.toByteArray()
    }
}
