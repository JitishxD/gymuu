package me.jitish.gymuu.ui

import me.jitish.gymuu.ui.exercise.formatRestCountdown
import me.jitish.gymuu.ui.exercise.parseRestTimeToSeconds
import org.junit.Assert.assertEquals
import org.junit.Test

class RestTimeParsingTest {
    @Test fun blankAndZeroMeanNoTimer() {
        assertEquals(0, parseRestTimeToSeconds(""))
        assertEquals(0, parseRestTimeToSeconds("0"))
        assertEquals(0, parseRestTimeToSeconds("0:00"))
        assertEquals(0, parseRestTimeToSeconds(":"))
    }

    @Test fun parsesPartialInput() {
        assertEquals(720, parseRestTimeToSeconds("12:"))
        assertEquals(30, parseRestTimeToSeconds(":30"))
        assertEquals(90, parseRestTimeToSeconds("1:30"))
        assertEquals(120, parseRestTimeToSeconds("2"))
    }

    @Test fun clampsToMax() = assertEquals(5999, parseRestTimeToSeconds("200:00"))

    @Test fun formatsWithoutLocaleDigits() {
        assertEquals("01:05", formatRestCountdown(65))
        assertEquals("00:00", formatRestCountdown(-3))
    }
}
