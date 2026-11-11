package com.lifestyle.nestly.ui.photo

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import kotlin.math.max
import kotlin.math.roundToInt

/** Decode, straighten, crop, and stage child photos as upright JPEGs. */
object PhotoImages {
    fun decodeUri(context: Context, uri: Uri, maxEdge: Int = 3072): Bitmap? = runCatching {
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return null
        val orientation = ExifInterface(ByteArrayInputStream(bytes)).orientation()
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        val opts = BitmapFactory.Options().apply {
            inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight, maxEdge)
        }
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts) ?: return null
        applyExif(bitmap, orientation)
    }.getOrNull()

    fun decodeFile(path: String, maxEdge: Int = 2048): Bitmap? = runCatching {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, bounds)
        val opts = BitmapFactory.Options().apply {
            inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight, maxEdge)
        }
        val bitmap = BitmapFactory.decodeFile(path, opts) ?: return null
        val orientation = ExifInterface(path).orientation()
        applyExif(bitmap, orientation)
    }.getOrNull()

    /** Visible square of [source] after the crop preview's cover-scale, user scale, and pan. */
    fun cropSquare(source: Bitmap, viewSize: Float, scale: Float, offsetX: Float, offsetY: Float): Bitmap {
        if (viewSize <= 0f) return source
        val userScale = scale.coerceIn(1f, 5f)
        val cover = max(viewSize / source.width, viewSize / source.height)
        val drawW = source.width * cover * userScale
        val drawH = source.height * cover * userScale
        val left = viewSize / 2f + offsetX - drawW / 2f
        val top = viewSize / 2f + offsetY - drawH / 2f
        val srcX = ((0f - left) / drawW * source.width).roundToInt().coerceIn(0, source.width - 1)
        val srcY = ((0f - top) / drawH * source.height).roundToInt().coerceIn(0, source.height - 1)
        val side = minOf(
            (viewSize / drawW * source.width).roundToInt().coerceAtLeast(1),
            (viewSize / drawH * source.height).roundToInt().coerceAtLeast(1),
            source.width - srcX,
            source.height - srcY,
        ).coerceAtLeast(1)
        return Bitmap.createBitmap(source, srcX, srcY, side, side)
    }

    /** Writes an upright JPEG (long edge ≤ 2048) and returns the local file path. */
    fun stageJpeg(context: Context, bitmap: Bitmap): String {
        val scaled = scaleDown(bitmap, 2048)
        val dir = File(context.filesDir, "photos").apply { mkdirs() }
        val file = File(dir, "${UUID.randomUUID()}.jpg")
        FileOutputStream(file).use { scaled.compress(Bitmap.CompressFormat.JPEG, 85, it) }
        if (scaled !== bitmap) scaled.recycle()
        return file.absolutePath
    }

    fun sampleSize(width: Int, height: Int, maxEdge: Int): Int {
        var sample = 1
        val edge = max(width, height)
        while (edge / (sample * 2) > maxEdge) sample *= 2
        return sample
    }

    private fun scaleDown(bitmap: Bitmap, maxEdge: Int): Bitmap {
        val edge = max(bitmap.width, bitmap.height)
        if (edge <= maxEdge) return bitmap
        val ratio = maxEdge.toFloat() / edge
        return Bitmap.createScaledBitmap(
            bitmap,
            (bitmap.width * ratio).roundToInt().coerceAtLeast(1),
            (bitmap.height * ratio).roundToInt().coerceAtLeast(1),
            true,
        )
    }

    private fun ExifInterface.orientation(): Int =
        getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)

    private fun applyExif(bitmap: Bitmap, orientation: Int): Bitmap {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.preScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.preScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.postRotate(90f)
                matrix.preScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.postRotate(270f)
                matrix.preScale(-1f, 1f)
            }
            else -> return bitmap
        }
        val turned = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        if (turned !== bitmap) bitmap.recycle()
        return turned
    }
}
