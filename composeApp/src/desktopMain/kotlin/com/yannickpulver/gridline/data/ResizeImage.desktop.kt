package com.yannickpulver.gridline.data

import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

// Desktop (JVM) Implementation (jvmMain)
actual fun resizeImage(inputBytes: ByteArray, maxSize: Int): ByteArray {
    val inputStream = ByteArrayInputStream(inputBytes)
    val originalImage = ImageIO.read(inputStream)

    var targetWidth = maxSize
    var targetHeight = maxSize

    if (originalImage.height < originalImage.width) {
        val ratio = originalImage.height.toDouble() / originalImage.width.toDouble()
        targetHeight = (targetWidth * ratio).toInt()
    } else {
        val ratio = originalImage.width.toDouble() / originalImage.height.toDouble()
        targetWidth = (targetHeight * ratio).toInt()
    }

    val resizedImage = BufferedImage(targetWidth, targetHeight, originalImage.type)
    val g2 = resizedImage.createGraphics()
    g2.drawImage(originalImage, 0, 0, targetWidth, targetHeight, null)
    g2.dispose()

    val baos = ByteArrayOutputStream()
    ImageIO.write(resizedImage, "jpg", baos)
    return baos.toByteArray()
}
