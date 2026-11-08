package com.plannext.app.util

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.util.TimeZone

class PlanTimeTest {
    private lateinit var originalTimeZone: TimeZone

    @Before
    fun setUp() {
        originalTimeZone = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Shanghai"))
    }

    @After
    fun tearDown() {
        TimeZone.setDefault(originalTimeZone)
    }

    @Test
    fun toMillis_roundTripsDateAndTime() {
        val date = LocalDate.of(2026, 11, 9)
        val time = LocalTime.of(18, 30)

        val millis = PlanTime.toMillis(date, time)

        assertEquals(date, PlanTime.toDate(millis))
        assertEquals(time, PlanTime.toTime(millis))
    }

    @Test
    fun relativeDay_usesNaturalChineseLabels() {
        val today = LocalDate.of(2026, 11, 8)

        assertEquals("今天", PlanTime.relativeDay(today, today))
        assertEquals("明天", PlanTime.relativeDay(today.plusDays(1), today))
        assertEquals("后天", PlanTime.relativeDay(today.plusDays(2), today))
        assertEquals("11月11日", PlanTime.relativeDay(today.plusDays(3), today))
    }

    @Test
    fun durationLabel_handlesHoursAndMinutes() {
        val start = PlanTime.toMillis(LocalDate.of(2026, 11, 9), LocalTime.of(9, 0))
        val end = PlanTime.toMillis(LocalDate.of(2026, 11, 9), LocalTime.of(10, 30))

        assertEquals("1小时30分钟", PlanTime.durationLabel(start, end))
    }

    @Test
    fun relativeRemainingLabel_handlesMinutesAndHours() {
        val now = PlanTime.toMillis(LocalDate.of(2026, 11, 9), LocalTime.of(9, 0))

        assertEquals(
            "30分钟后",
            PlanTime.relativeRemainingLabel(now + 30 * 60_000L, now),
        )
        assertEquals(
            "2小时后",
            PlanTime.relativeRemainingLabel(now + 2 * 60 * 60_000L, now),
        )
    }
}

