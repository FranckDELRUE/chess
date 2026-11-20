package com.chesscompanion.feature.training

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant
import java.time.ZoneId

class ReviewReminderTest {
    private val paris = ZoneId.of("Europe/Paris")

    @Test
    fun theDelayRunsToTodaysReminderHourWhenItIsAhead() {
        val now = Instant.parse("2026-11-20T10:00:00Z") // 11:00 in Paris
        assertEquals(Duration.ofHours(7), delayUntilNext(now, paris))
    }

    @Test
    fun theDelayRunsToTomorrowOnceTheHourIsPast() {
        val now = Instant.parse("2026-11-20T18:00:00Z") // 19:00 in Paris
        assertEquals(Duration.ofHours(23), delayUntilNext(now, paris))
    }

    @Test
    fun exactlyAtTheHourWaitsAFullDay() {
        val now = Instant.parse("2026-11-20T17:00:00Z") // 18:00 in Paris
        assertEquals(Duration.ofHours(24), delayUntilNext(now, paris))
    }
}
