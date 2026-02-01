package com.yannickpulver.gridline.data

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

actual fun resizeImage(inputBytes: ByteArray, maxSize: Int, extension: String): ByteArray {
    val exifOrientation = getExifOrientation(inputBytes)
    var bitmap = BitmapFactory.decodeByteArray(inputBytes, 0, inputBytes.size)
    bitmap = applyExifRotation(bitmap, exifOrientation)

    var targetWidth = maxSize
    var targetHeight = maxSize

    if (bitmap.height < bitmap.width) {
        val ratio = bitmap.height.toDouble() / bitmap.width.toDouble()
        targetHeight = (targetWidth * ratio).toInt()
    } else {
        val ratio = bitmap.width.toDouble() / bitmap.height.toDouble()
        targetWidth = (targetHeight * ratio).toInt()
    }

    val resizedBitmap = Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
    val stream = ByteArrayOutputStream()
    resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
    return stream.toByteArray()
}

private fun getExifOrientation(bytes: ByteArray): Int {
    return try {
        val inputStream = ByteArrayInputStream(bytes)
        val exif = ExifInterface(inputStream)
        exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
    } catch (e: Exception) {
        ExifInterface.ORIENTATION_NORMAL
    }
}

private fun applyExifRotation(bitmap: Bitmap, orientation: Int): Bitmap {
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
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
}
