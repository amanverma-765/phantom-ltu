package com.navi.phantom.core.ext

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.io.ByteArrayOutputStream

/**
 * Convert a Drawable to a ByteArray (PNG format).
 *
 * @param maxSize Maximum width/height for the output bitmap (default 96px for icons)
 * @param quality PNG compression quality (0-100, default 100)
 * @return ByteArray containing the PNG data, or null if conversion fails
 */
fun Drawable.toByteArray(maxSize: Int = 96, quality: Int = 100): ByteArray? {
    return try {
        val bitmap = toBitmap(maxSize)
        ByteArrayOutputStream().use { stream ->
            bitmap.compress(Bitmap.CompressFormat.PNG, quality, stream)
            stream.toByteArray()
        }
    } catch (e: Exception) {
        null
    }
}

/**
 * Convert a Drawable to a Bitmap.
 *
 * @param maxSize Maximum width/height for the output bitmap
 * @return Bitmap representation of the drawable
 */
fun Drawable.toBitmap(maxSize: Int = 96): Bitmap {
    if (this is BitmapDrawable && bitmap != null) {
        return if (bitmap.width <= maxSize && bitmap.height <= maxSize) {
            bitmap
        } else {
            Bitmap.createScaledBitmap(bitmap, maxSize, maxSize, true)
        }
    }

    val width = if (intrinsicWidth > 0) intrinsicWidth else maxSize
    val height = if (intrinsicHeight > 0) intrinsicHeight else maxSize

    val scaledWidth: Int
    val scaledHeight: Int
    if (width > maxSize || height > maxSize) {
        val scale = minOf(maxSize.toFloat() / width, maxSize.toFloat() / height)
        scaledWidth = (width * scale).toInt()
        scaledHeight = (height * scale).toInt()
    } else {
        scaledWidth = width
        scaledHeight = height
    }

    val bitmap = Bitmap.createBitmap(scaledWidth, scaledHeight, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    setBounds(0, 0, scaledWidth, scaledHeight)
    draw(canvas)
    return bitmap
}

/**
 * Convert a ByteArray (PNG/JPEG) to a Bitmap.
 *
 * @return Bitmap or null if decoding fails
 */
fun ByteArray.toBitmap(): Bitmap? {
    return try {
        BitmapFactory.decodeByteArray(this, 0, size)
    } catch (e: Exception) {
        null
    }
}

/**
 * Convert a ByteArray (PNG/JPEG) to an ImageBitmap for Compose.
 *
 * @return ImageBitmap or null if decoding fails
 */
fun ByteArray.toImageBitmap(): ImageBitmap? {
    return toBitmap()?.asImageBitmap()
}
