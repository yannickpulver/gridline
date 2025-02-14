package com.yannickpulver.gridline.data

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.allocArrayOf
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.useContents
import kotlinx.cinterop.usePinned
import platform.CoreGraphics.*
import platform.Foundation.*
import platform.UIKit.*
import platform.posix.memcpy

@OptIn(ExperimentalForeignApi::class)
actual fun resizeImage(inputBytes: ByteArray, maxSize: Int, extension: String): ByteArray {
    val data = inputBytes.toNSData()
    val image = UIImage(data = data) ?: return inputBytes

    val (w, h) = image.size.useContents { width to height }
    val ratio = w / h
    var (targetW, targetH) = maxSize to maxSize
    if (w > h) targetH = (maxSize / ratio).toInt() else targetW = (maxSize * ratio).toInt()

    UIGraphicsBeginImageContextWithOptions(
        size = CGSizeMake(targetW.toDouble(), targetH.toDouble()),
        opaque = false,
        scale = 1.0
    )
    image.drawInRect(CGRectMake(0.0, 0.0, targetW.toDouble(), targetH.toDouble()))
    val resized = UIGraphicsGetImageFromCurrentImageContext()
    UIGraphicsEndImageContext()

    resized ?: return byteArrayOf()

    return if (extension.lowercase() == "png")
        UIImagePNGRepresentation(resized)?.toByteArray() ?: return byteArrayOf()
    else
        UIImageJPEGRepresentation(resized, 0.85)?.toByteArray() ?: return byteArrayOf()
}

@OptIn(ExperimentalForeignApi::class)
fun ByteArray.toNSData(): NSData = memScoped {
    NSData.create(bytes = allocArrayOf(this@toNSData), length = this@toNSData.size.toULong())
}
@OptIn(ExperimentalForeignApi::class)
fun NSData.toByteArray(): ByteArray {
    return ByteArray(length.toInt()).apply {
        usePinned {
            memcpy(it.addressOf(0), bytes, length)
        }
    }
}