package com.yannickpulver.gridline.data

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.ByteArrayOutputStream

// Android Implementation (androidMain)
actual fun resizeImage(inputBytes: ByteArray, maxSize: Int, extension: String): ByteArray {
    val bitmap = BitmapFactory.decodeByteArray(inputBytes, 0, inputBytes.size)

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
