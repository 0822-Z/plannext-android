package com.plannext.app.ui

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class WeekPlannerLogicTest {
    @Test
    fun mondayOf_sundayReturnsStartOfWeek() {
        val sunday = LocalDate.of(2026, 11, 8)

        assertEquals(LocalDate.of(2026, 11, 2), mondayOf(sunday))
    }

    @Test
    fun mondayOf_mondayIsUnchanged() {
        val monday = LocalDate.of(2026, 11, 2)

        assertEquals(monday, mondayOf(monday))
    }

    @Test
    fun defaultPlanTimes_forFutureDateStartsAtNine() {
        val date = LocalDate.of(2026, 11, 9)

        val times = defaultPlanTimes(
            date = date,
            now = LocalDateTime.of(date.minusDays(1), LocalTime.of(23, 0)),
        )

        assertEquals(LocalTime.of(9, 0), times.first)
        assertEquals(LocalTime.of(10, 0), times.second)
    }

    @Test
    fun defaultPlanTimes_forTodayUsesNextHalfHour() {
        val date = LocalDate.of(2026, 11, 8)

        val times = defaultPlanTimes(
            date = date,
            now = LocalDateTime.of(date, LocalTime.of(9, 7)),
        )

        assertEquals(LocalTime.of(9, 30), times.first)
        assertEquals(LocalTime.of(10, 30), times.second)
    }
}

