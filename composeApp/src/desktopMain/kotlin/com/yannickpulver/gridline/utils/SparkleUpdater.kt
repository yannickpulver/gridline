package com.yannickpulver.gridline.utils

import com.sun.jna.Callback
import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.NativeLibrary
import com.sun.jna.Pointer
import java.io.File

/**
 * Updates the GitHub (DMG) build with Sparkle. The framework sits in Contents/Frameworks,
 * so this does nothing in the App Store build or when running from Gradle.
 */
object SparkleUpdater {

    // Set by the jpackage launcher: <app>/Contents/MacOS/Gridline
    private val framework = System.getProperty("jpackage.app-path")
        ?.let { File(it).parentFile.resolveSibling("Frameworks/Sparkle.framework/Sparkle") }

    val isAvailable = framework?.exists() == true

    private val objc by lazy { Native.load("objc", ObjC::class.java) }
    private var controller: Pointer? = null

    // Sparkle only runs on the AppKit main thread, which isn't the Swing thread
    private val start = Work {
        NativeLibrary.getInstance(framework!!.path)
        val alloc = objc.objc_msgSend(objc.objc_getClass("SPUStandardUpdaterController"), objc.sel_registerName("alloc"))
        controller = objc.objc_msgSend(
            alloc,
            objc.sel_registerName("initWithStartingUpdater:updaterDelegate:userDriverDelegate:"),
            true,
            null,
            null,
        )
    }
    private val check = Work {
        controller?.let { objc.objc_msgSend(it, objc.sel_registerName("checkForUpdates:"), null) }
    }

    /** Starts the scheduled update checks. */
    fun start() {
        if (isAvailable) onMainThread(start)
    }

    fun checkForUpdates() {
        if (isAvailable) onMainThread(check)
    }

    private fun onMainThread(work: Work) {
        val mainQueue = NativeLibrary.getInstance("System").getGlobalVariableAddress("_dispatch_main_q")
        Native.load("System", Dispatch::class.java).dispatch_async_f(mainQueue, null, work)
    }

    private fun interface Work : Callback {
        fun invoke(context: Pointer?)
    }

    @Suppress("FunctionName")
    private interface ObjC : Library {
        fun objc_getClass(name: String): Pointer
        fun sel_registerName(name: String): Pointer
        fun objc_msgSend(receiver: Pointer, selector: Pointer): Pointer
        fun objc_msgSend(receiver: Pointer, selector: Pointer, sender: Pointer?): Pointer?
        fun objc_msgSend(receiver: Pointer, selector: Pointer, start: Boolean, updaterDelegate: Pointer?, userDriverDelegate: Pointer?): Pointer
    }

    @Suppress("FunctionName")
    private interface Dispatch : Library {
        fun dispatch_async_f(queue: Pointer, context: Pointer?, work: Work)
    }
}
