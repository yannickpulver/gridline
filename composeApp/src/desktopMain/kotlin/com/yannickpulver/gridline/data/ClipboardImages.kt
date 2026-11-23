package com.yannickpulver.gridline.data

import java.awt.Image
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.awt.image.BufferedImage
import java.awt.image.MultiResolutionImage
import java.io.ByteArrayOutputStream
import java.io.File
import javax.imageio.ImageIO

private val imageExtensions = setOf("jpg", "jpeg", "png")

/** Images on the clipboard: files copied in Finder, or image data copied from Preview, Photos or a browser. */
fun readClipboardImages(): List<Pair<ByteArray, String>> = runCatching {
    val clipboard = Toolkit.getDefaultToolkit().systemClipboard
    when {
        clipboard.isDataFlavorAvailable(DataFlavor.javaFileListFlavor) ->
            (clipboard.getData(DataFlavor.javaFileListFlavor) as List<*>)
                .filterIsInstance<File>()
                .filter { it.extension.lowercase() in imageExtensions }
                .map { it.readBytes() to it.extension.lowercase() }

        clipboard.isDataFlavorAvailable(DataFlavor.imageFlavor) ->
            listOf((clipboard.getData(DataFlavor.imageFlavor) as Image).toJpg() to "jpg")

        else -> emptyList()
    }
}.getOrDefault(emptyList())

private fun Image.toJpg(): ByteArray {
    val image = if (this is MultiResolutionImage) resolutionVariants.maxBy { it.getWidth(null) } else this
    val rgb = BufferedImage(image.getWidth(null), image.getHeight(null), BufferedImage.TYPE_INT_RGB)
    rgb.createGraphics().run { drawImage(image, 0, 0, java.awt.Color.WHITE, null); dispose() }
    return ByteArrayOutputStream().also { ImageIO.write(rgb, "jpg", it) }.toByteArray()
}
