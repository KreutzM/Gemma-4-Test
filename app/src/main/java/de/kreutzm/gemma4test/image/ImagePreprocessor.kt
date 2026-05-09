package de.kreutzm.gemma4test.image

import android.graphics.Bitmap
import java.io.ByteArrayOutputStream

object ImagePreprocessor {
    const val DEFAULT_MAX_LONG_EDGE_PX = 1024

    fun scaleToMaxLongEdge(
        source: Bitmap,
        maxLongEdgePx: Int = DEFAULT_MAX_LONG_EDGE_PX,
    ): Bitmap {
        val targetSize = ImageResizePlanner.planScaleToMaxLongEdge(
            source = ImageSize(source.width, source.height),
            maxLongEdgePx = maxLongEdgePx,
        )

        if (targetSize.width == source.width && targetSize.height == source.height) return source

        return Bitmap.createScaledBitmap(source, targetSize.width, targetSize.height, true)
    }

    fun toPngBytes(bitmap: Bitmap): ByteArray {
        val output = ByteArrayOutputStream()
        check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)) { "Could not encode bitmap as PNG" }
        return output.toByteArray()
    }
}
