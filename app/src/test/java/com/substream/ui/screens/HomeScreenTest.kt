package com.substream.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Guards the home greeting buckets. The hour comes from the device clock, so the boundaries
 * are the only thing that can silently drift.
 */
class HomeScreenTest {

    @Test
    fun `greeting follows the time of day`() {
        assertEquals("Good evening", greetingFor(0))
        assertEquals("Good morning", greetingFor(5))
        assertEquals("Good morning", greetingFor(11))
        assertEquals("Good afternoon", greetingFor(12))
        assertEquals("Good afternoon", greetingFor(17))
        assertEquals("Good evening", greetingFor(18))
        assertEquals("Good evening", greetingFor(23))
    }
}