package com.plannext.app.util

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

object PlanTime {
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.CHINA)
    private val dateFormatter = DateTimeFormatter.ofPattern("M月d日", Locale.CHINA)
    private val fullDateFormatter = DateTimeFormatter.ofPattern("yyyy年M月d日", Locale.CHINA)

    fun zone(): ZoneId = ZoneId.systemDefault()

    fun toDate(millis: Long): LocalDate =
        Instant.ofEpochMilli(millis).atZone(zone()).toLocalDate()

    fun toTime(millis: Long): LocalTime =
        Instant.ofEpochMilli(millis).atZone(zone()).toLocalTime()

    fun toMillis(date: LocalDate, time: LocalTime): Long =
        date.atTime(time).atZone(zone()).toInstant().toEpochMilli()

    fun formatTime(millis: Long): String = toTime(millis).format(timeFormatter)

    fun formatDate(date: LocalDate): String = date.format(dateFormatter)

    fun formatFullDate(date: LocalDate): String = date.format(fullDateFormatter)

    fun weekday(date: LocalDate): String = when (date.dayOfWeek.value) {
        1 -> "周一"
        2 -> "周二"
        3 -> "周三"
        4 -> "周四"
        5 -> "周五"
        6 -> "周六"
        else -> "周日"
    }

    fun relativeDay(date: LocalDate, today: LocalDate = LocalDate.now()): String = when (date) {
        today -> "今天"
        today.plusDays(1) -> "明天"
        today.plusDays(2) -> "后天"
        else -> formatDate(date)
    }

    fun durationMinutes(startAtMillis: Long, endAtMillis: Long): Long =
        ChronoUnit.MINUTES.between(
            Instant.ofEpochMilli(startAtMillis),
            Instant.ofEpochMilli(endAtMillis),
        ).coerceAtLeast(1)

    fun durationLabel(startAtMillis: Long, endAtMillis: Long): String {
        val minutes = durationMinutes(startAtMillis, endAtMillis)
        val hours = minutes / 60
        val remainingMinutes = minutes % 60
        return when {
            hours > 0 && remainingMinutes > 0 -> "${hours}小时${remainingMinutes}分钟"
            hours > 0 -> "${hours}小时"
            else -> "${remainingMinutes}分钟"
        }
    }

    fun relativeRemainingLabel(startAtMillis: Long, nowMillis: Long = System.currentTimeMillis()): String {
        val minutes = ((startAtMillis - nowMillis) / 60_000L).coerceAtLeast(0)
        return when {
            minutes < 1 -> "即将开始"
            minutes < 60 -> "${minutes}分钟后"
            minutes < 24 * 60 -> "${minutes / 60}小时后"
            else -> "${minutes / (24 * 60)}天后"
        }
    }
}

