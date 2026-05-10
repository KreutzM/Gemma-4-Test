package de.kreutzm.gemma4test.image

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import java.io.ByteArrayOutputStream

object ImagePreprocessor {
    const val DEFAULT_MAX_LONG_EDGE_PX = 512
    const val DEFAULT_LETTERBOX_SIZE_PX = 512

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

    fun letterboxToSquare(
        source: Bitmap,
        squareSizePx: Int = DEFAULT_LETTERBOX_SIZE_PX,
    ): Bitmap {
        val plan = ImageResizePlanner.planLetterboxSquare(
            source = ImageSize(source.width, source.height),
            squareSizePx = squareSizePx,
        )
        val scaled = if (plan.scaledImageSize.width == source.width && plan.scaledImageSize.height == source.height) {
            source
        } else {
            Bitmap.createScaledBitmap(source, plan.scaledImageSize.width, plan.scaledImageSize.height, true)
        }

        val output = Bitmap.createBitmap(plan.canvasSize.width, plan.canvasSize.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        canvas.drawColor(Color.BLACK)
        canvas.drawBitmap(scaled, plan.offsetX.toFloat(), plan.offsetY.toFloat(), Paint(Paint.FILTER_BITMAP_FLAG))
        return output
    }

    fun toPngBytes(bitmap: Bitmap): ByteArray {
        val output = ByteArrayOutputStream()
        check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)) { "Could not encode bitmap as PNG" }
        return output.toByteArray()
    }
}
