package com.yannickpulver.gridline.ui.feed

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ShortcodeTest {

    @Test
    fun readsShortcodeFromPostLinks() {
        assertEquals("DfMQRQdCteW", shortcodeOf("https://www.instagram.com/p/DfMQRQdCteW/"))
        assertEquals("DfMQRQdCteW", shortcodeOf(" https://www.instagram.com/p/DfMQRQdCteW/?igsh=abc123 "))
        assertEquals("DfMQRQdCteW", shortcodeOf("https://www.instagram.com/yannickpulver/p/DfMQRQdCteW/"))
        assertEquals("C1a-b_2", shortcodeOf("https://www.instagram.com/reel/C1a-b_2/"))
    }

    @Test
    fun acceptsBareShortcode() {
        assertEquals("DfMQRQdCteW", shortcodeOf("DfMQRQdCteW"))
    }

    @Test
    fun rejectsOtherText() {
        assertNull(shortcodeOf("https://www.instagram.com/yannickpulver/"))
        assertNull(shortcodeOf("not a link"))
    }
}
