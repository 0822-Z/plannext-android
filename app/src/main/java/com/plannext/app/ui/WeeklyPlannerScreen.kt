package com.plannext.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.plannext.app.PermissionSnapshot
import com.plannext.app.data.Plan
import com.plannext.app.data.PlanCategory
import com.plannext.app.ui.theme.Accent
import com.plannext.app.ui.theme.AccentSoft
import com.plannext.app.ui.theme.CardWhite
import com.plannext.app.ui.theme.Cream
import com.plannext.app.ui.theme.Ink
import com.plannext.app.ui.theme.Line
import com.plannext.app.ui.theme.MutedInk
import com.plannext.app.ui.theme.Night
import com.plannext.app.ui.theme.PlanNextTheme
import com.plannext.app.ui.theme.WarmWhite
import com.plannext.app.util.PlanTime
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt

private val HourHeight = 58.dp
private val TimeAxisWidth = 34.dp
private const val VisibleStartHour = 8
private const val VisibleEndHour = 23
private const val VisibleMinutes = (VisibleEndHour - VisibleStartHour) * 60

@Composable
fun WeekPlannerScreen(
    plans: List<Plan>,
    weekStart: LocalDate,
    permissions: PermissionSnapshot,
    onPreviousWeek: () -> Unit,
    onNextWeek: () -> Unit,
    onCurrentWeek: () -> Unit,
    onOpenSlot: (LocalDate, LocalTime, LocalTime) -> Unit,
    onEditPlan: (Plan) -> Unit,
    onRequestNotifications: () -> Unit,
    onOpenExactAlarmSettings: () -> Unit,
) {
    val weekDates = remember(weekStart) { (0L..6L).map(weekStart::plusDays) }
    val weekEnd = weekDates.last()
    val isCurrentWeek = weekStart == mondayOf(LocalDate.now())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Cream),
    ) {
        WeekToolbar(
            weekStart = weekStart,
            weekEnd = weekEnd,
            isCurrentWeek = isCurrentWeek,
            onPreviousWeek = onPreviousWeek,
            onNextWeek = onNextWeek,
            onCurrentWeek = onCurrentWeek,
        )

        if (!permissions.notificationsEnabled || !permissions.exactAlarmsEnabled) {
            PermissionStrip(
                permissions = permissions,
                onRequestNotifications = onRequestNotifications,
                onOpenExactAlarmSettings = onOpenExactAlarmSettings,
            )
        }

        DayHeader(weekDates = weekDates)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 10.dp),
        ) {
            WeekGrid(
                plans = plans,
                weekStart = weekStart,
                onOpenSlot = onOpenSlot,
                onEditPlan = onEditPlan,
            )

            Surface(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .navigationBarsPadding()
                    .padding(end = 18.dp, bottom = 18.dp)
                    .size(56.dp)
                    .clickable {
                        val slot = quickAddSlot()
                        onOpenSlot(slot.first, slot.second, slot.third)
                    },
                shape = CircleShape,
                color = Night,
                shadowElevation = 10.dp,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "＋",
                        color = Color.White,
                        fontSize = 25.sp,
                        fontWeight = FontWeight.Light,
                    )
                }
            }
        }
    }
}

@Composable
private fun WeekToolbar(
    weekStart: LocalDate,
    weekEnd: LocalDate,
    isCurrentWeek: Boolean,
    onPreviousWeek: () -> Unit,
    onNextWeek: () -> Unit,
    onCurrentWeek: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BrandMark()
            Spacer(Modifier.width(9.dp))
            Column {
                Text(
                    text = "有期",
                    style = MaterialTheme.typography.titleLarge,
                    color = Ink,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "让每一天，都有所期待",
                    color = MutedInk,
                    fontSize = 10.sp,
                    letterSpacing = 0.4.sp,
                )
            }
            Spacer(Modifier.weight(1f))
            if (!isCurrentWeek) {
                Surface(
                    modifier = Modifier.clickable(onClick = onCurrentWeek),
                    shape = CircleShape,
                    color = AccentSoft,
                ) {
                    Text(
                        text = "回到本周",
                        color = Accent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text = weekRangeLabel(weekStart, weekEnd),
                    color = Ink,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "七日光阴 · 慢慢安排",
                    color = MutedInk,
                    fontSize = 10.sp,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            Spacer(Modifier.weight(1f))
            ToolbarButton(text = "‹", onClick = onPreviousWeek)
            Spacer(Modifier.width(7.dp))
            ToolbarButton(text = "今", onClick = onCurrentWeek)
            Spacer(Modifier.width(7.dp))
            ToolbarButton(text = "›", onClick = onNextWeek)
        }
    }
}

@Composable
private fun BrandMark() {
    Canvas(modifier = Modifier.size(31.dp)) {
        drawRoundRect(
            color = Night,
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(8.dp.toPx()),
        )
        val ink = Color(0xFFF5EDE1)
        drawLine(
            color = ink,
            start = Offset(10.dp.toPx(), 8.dp.toPx()),
            end = Offset(10.dp.toPx(), 12.dp.toPx()),
            strokeWidth = 1.5.dp.toPx(),
            cap = StrokeCap.Round,
        )
        drawLine(
            color = ink,
            start = Offset(21.dp.toPx(), 8.dp.toPx()),
            end = Offset(21.dp.toPx(), 12.dp.toPx()),
            strokeWidth = 1.5.dp.toPx(),
            cap = StrokeCap.Round,
        )
        drawLine(
            color = ink.copy(alpha = 0.9f),
            start = Offset(8.dp.toPx(), 15.dp.toPx()),
            end = Offset(23.dp.toPx(), 15.dp.toPx()),
            strokeWidth = 1.2.dp.toPx(),
        )
        val check = Path().apply {
            moveTo(10.dp.toPx(), 20.dp.toPx())
            lineTo(14.dp.toPx(), 24.dp.toPx())
            lineTo(22.dp.toPx(), 16.dp.toPx())
        }
        drawPath(
            path = check,
            color = Accent,
            style = Stroke(width = 1.7.dp.toPx(), cap = StrokeCap.Round),
        )
    }
}

@Composable
private fun ToolbarButton(text: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .size(31.dp)
            .clickable(onClick = onClick),
        shape = CircleShape,
        color = CardWhite,
        border = androidx.compose.foundation.BorderStroke(1.dp, Line),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = text,
                color = Ink,
                fontSize = if (text == "‹" || text == "›") 20.sp else 11.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
private fun PermissionStrip(
    permissions: PermissionSnapshot,
    onRequestNotifications: () -> Unit,
    onOpenExactAlarmSettings: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp)
            .clickable {
                if (!permissions.notificationsEnabled) {
                    onRequestNotifications()
                } else {
                    onOpenExactAlarmSettings()
                }
            },
        shape = RoundedCornerShape(11.dp),
        color = Color(0xFFFBEEE6),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(Accent, CircleShape),
            )
            Text(
                text = if (!permissions.notificationsEnabled) {
                    "提醒权限尚未开启，轻点设置"
                } else {
                    "精确提醒尚未开启，轻点设置"
                },
                color = Accent,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}

@Composable
private fun DayHeader(weekDates: List<LocalDate>) {
    val today = LocalDate.now()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        Spacer(Modifier.width(TimeAxisWidth))
        weekDates.forEach { date ->
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = weekdayShort(date),
                    color = if (date == today) Accent else MutedInk,
                    fontSize = 10.sp,
                    fontWeight = if (date == today) FontWeight.SemiBold else FontWeight.Normal,
                )
                Spacer(Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .background(
                            if (date == today) Ink else Color.Transparent,
                            CircleShape,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = date.dayOfMonth.toString(),
                        color = if (date == today) Color.White else Ink,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

@Composable
private fun WeekGrid(
    plans: List<Plan>,
    weekStart: LocalDate,
    onOpenSlot: (LocalDate, LocalTime, LocalTime) -> Unit,
    onEditPlan: (Plan) -> Unit,
) {
    val positionedPlans = remember(plans, weekStart) {
        layoutPlansForWeek(plans, weekStart)
    }
    val scrollState = rememberScrollState()
    val currentDateTime by produceState(initialValue = LocalDateTime.now()) {
        while (true) {
            value = LocalDateTime.now()
            delay(60_000)
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .background(WarmWhite),
    ) {
        val totalHeight = HourHeight * (VisibleEndHour - VisibleStartHour)
        val gridWidth = maxWidth - TimeAxisWidth
        val columnWidth = gridWidth / 7f
        val todayIndex = ChronoUnit.DAYS.between(weekStart, currentDateTime.toLocalDate()).toInt()

        Box(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(totalHeight),
            ) {
                TimeAxis(hourHeight = HourHeight, totalHeight = totalHeight)

                Box(
                    modifier = Modifier
                        .width(gridWidth)
                        .height(totalHeight),
                ) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(weekStart, gridWidth, totalHeight) {
                                detectTapGestures { offset ->
                                    val dayIndex = ((offset.x / size.width) * 7)
                                        .toInt()
                                        .coerceIn(0, 6)
                                    val rawMinutes = VisibleStartHour * 60 +
                                        ((offset.y / size.height) * VisibleMinutes)
                                        .roundToInt()
                                    val startMinutes = (rawMinutes / 30f)
                                        .roundToInt()
                                        .times(30)
                                        .coerceIn(
                                            VisibleStartHour * 60,
                                            VisibleEndHour * 60 - 30,
                                        )
                                    val endMinutes = (startMinutes + 60)
                                        .coerceAtMost(VisibleEndHour * 60)
                                    onOpenSlot(
                                        weekStart.plusDays(dayIndex.toLong()),
                                        minutesToLocalTime(startMinutes),
                                        minutesToLocalTime(endMinutes),
                                    )
                                }
                            },
                    ) {
                        if (todayIndex in 0..6) {
                            drawRect(
                                color = Accent.copy(alpha = 0.045f),
                                topLeft = Offset(columnWidth.toPx() * todayIndex, 0f),
                                size = androidx.compose.ui.geometry.Size(
                                    width = columnWidth.toPx(),
                                    height = size.height,
                                ),
                            )
                        }

                        for (hour in 0..(VisibleEndHour - VisibleStartHour)) {
                            val y = HourHeight.toPx() * hour
                            drawLine(
                                color = if (hour == 0 || hour == VisibleEndHour - VisibleStartHour) {
                                    Line
                                } else {
                                    Line.copy(alpha = 0.78f)
                                },
                                start = Offset(0f, y),
                                end = Offset(size.width, y),
                                strokeWidth = if (hour % 2 == 0) 1.dp.toPx() else 0.6.dp.toPx(),
                            )
                            if (hour < VisibleEndHour - VisibleStartHour) {
                                val halfY = y + HourHeight.toPx() / 2
                                drawLine(
                                    color = Line.copy(alpha = 0.34f),
                                    start = Offset(0f, halfY),
                                    end = Offset(size.width, halfY),
                                    strokeWidth = 0.5.dp.toPx(),
                                )
                            }
                        }

                        for (day in 0..7) {
                            val x = columnWidth.toPx() * day
                            drawLine(
                                color = if (day == 0 || day == 7) {
                                    Line
                                } else {
                                    Line.copy(alpha = 0.8f)
                                },
                                start = Offset(x, 0f),
                                end = Offset(x, size.height),
                                strokeWidth = 0.8.dp.toPx(),
                            )
                        }

                        if (todayIndex in 0..6 &&
                            currentDateTime.toLocalDate() == weekStart.plusDays(todayIndex.toLong())
                        ) {
                            val minutes = currentDateTime.hour * 60 + currentDateTime.minute
                            val y = size.height *
                                ((minutes - VisibleStartHour * 60) / VisibleMinutes.toFloat())
                            val xStart = columnWidth.toPx() * todayIndex
                            if (minutes in VisibleStartHour * 60..VisibleEndHour * 60) {
                                drawLine(
                                    color = Accent,
                                    start = Offset(xStart, y),
                                    end = Offset(xStart + columnWidth.toPx(), y),
                                    strokeWidth = 1.3.dp.toPx(),
                                )
                                drawCircle(
                                    color = Accent,
                                    radius = 2.4.dp.toPx(),
                                    center = Offset(xStart + 2.dp.toPx(), y),
                                )
                            }
                        }
                    }

                    if (positionedPlans.isEmpty()) {
                        Text(
                            text = "这一周还没有安排\n轻点任意时间格，为未来留一个位置",
                            color = MutedInk.copy(alpha = 0.62f),
                            fontSize = 11.sp,
                            lineHeight = 17.sp,
                            modifier = Modifier
                                .offset(y = HourHeight * 2 - 6.dp)
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp),
                        )
                    }

                    positionedPlans.forEach { item ->
                        val widthFraction = item.columnWidthFraction
                        val x = columnWidth * item.dayIndex +
                            columnWidth * (item.column / widthFraction)
                        val y = totalHeight *
                            ((item.startMinutes - VisibleStartHour * 60) / VisibleMinutes.toFloat())
                        val height = (totalHeight *
                            ((item.endMinutes - item.startMinutes) / VisibleMinutes.toFloat()))
                            .coerceAtLeast(25.dp)
                        val style = eventStyle(item.plan.category)

                        Box(
                            modifier = Modifier
                                .offset(x = x + 1.dp, y = y + 1.dp)
                                .width((columnWidth / widthFraction - 2.dp).coerceAtLeast(6.dp))
                                .height(height - 2.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(style.background.copy(alpha = if (item.plan.isCompleted) 0.55f else 1f))
                                .clickable { onEditPlan(item.plan) },
                        ) {
                            Row(modifier = Modifier.fillMaxSize()) {
                                Box(
                                    modifier = Modifier
                                        .width(2.dp)
                                        .fillMaxHeight()
                                        .background(style.accent),
                                )
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 3.dp, vertical = 2.5.dp),
                                ) {
                                    Text(
                                        text = item.plan.title,
                                        color = style.text,
                                        fontSize = 8.sp,
                                        lineHeight = 10.sp,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = if (height >= 43.dp) 3 else 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    if (height >= 42.dp && columnWidth >= 40.dp) {
                                        Text(
                                            text = PlanTime.formatTime(item.plan.startAtMillis),
                                            color = style.text.copy(alpha = 0.68f),
                                            fontSize = 7.sp,
                                            lineHeight = 9.sp,
                                            modifier = Modifier.padding(top = 1.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TimeAxis(hourHeight: Dp, totalHeight: Dp) {
    Box(
        modifier = Modifier
            .width(TimeAxisWidth)
            .height(totalHeight),
    ) {
        for (hour in VisibleStartHour until VisibleEndHour step 2) {
            Text(
                text = hour.toString().padStart(2, '0'),
                color = MutedInk.copy(alpha = 0.72f),
                fontSize = 9.sp,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(y = (hourHeight * (hour - VisibleStartHour)) - 5.dp)
                    .padding(end = 5.dp),
            )
        }
        Text(
            text = VisibleEndHour.toString().padStart(2, '0'),
            color = MutedInk.copy(alpha = 0.72f),
            fontSize = 9.sp,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 5.dp, bottom = 1.dp),
        )
    }
}

private data class EventStyle(
    val background: Color,
    val text: Color,
    val accent: Color,
)

private fun eventStyle(category: PlanCategory): EventStyle = when (category) {
    PlanCategory.FOCUS -> EventStyle(Color(0xFFF0DDD5), Color(0xFF7C3E2E), Accent)
    PlanCategory.WORK -> EventStyle(Color(0xFFDCE6EC), Color(0xFF38566A), Color(0xFF668BA3))
    PlanCategory.STUDY -> EventStyle(Color(0xFFE8DEEE), Color(0xFF624D70), Color(0xFF8B6F9C))
    PlanCategory.LIFE -> EventStyle(Color(0xFFF1E4D4), Color(0xFF795831), Color(0xFFB6814E))
    PlanCategory.SPORT -> EventStyle(Color(0xFFDDE9DF), Color(0xFF3F6449), Color(0xFF6A9476))
}

private data class DayPlan(
    val plan: Plan,
    val startMinutes: Int,
    val endMinutes: Int,
)

private data class PositionedPlan(
    val plan: Plan,
    val dayIndex: Int,
    val startMinutes: Int,
    val endMinutes: Int,
    val column: Int,
    val columnWidthFraction: Float,
)

private fun layoutPlansForWeek(plans: List<Plan>, weekStart: LocalDate): List<PositionedPlan> {
    val result = mutableListOf<PositionedPlan>()

    for (dayIndex in 0..6) {
        val date = weekStart.plusDays(dayIndex.toLong())
        val dayPlans = plans
            .filter { PlanTime.toDate(it.startAtMillis) == date }
            .mapNotNull { plan ->
                val planStart = PlanTime.toTime(plan.startAtMillis).let { it.hour * 60 + it.minute }
                val planEnd = PlanTime.toTime(plan.endAtMillis).let { it.hour * 60 + it.minute }
                val visibleStart = maxOf(planStart, VisibleStartHour * 60)
                val visibleEnd = minOf(planEnd, VisibleEndHour * 60)
                if (visibleStart >= visibleEnd) {
                    null
                } else {
                    DayPlan(
                        plan = plan,
                        startMinutes = visibleStart,
                        endMinutes = visibleEnd,
                    )
                }
            }
            .sortedWith(compareBy<DayPlan> { it.startMinutes }.thenBy { it.endMinutes })

        val groups = mutableListOf<MutableList<DayPlan>>()
        var currentGroup = mutableListOf<DayPlan>()
        var groupEnd = -1
        dayPlans.forEach { item ->
            if (currentGroup.isEmpty() || item.startMinutes < groupEnd) {
                currentGroup.add(item)
                groupEnd = maxOf(groupEnd, item.endMinutes)
            } else {
                groups.add(currentGroup)
                currentGroup = mutableListOf(item)
                groupEnd = item.endMinutes
            }
        }
        if (currentGroup.isNotEmpty()) groups.add(currentGroup)

        groups.forEach { group ->
            val columnEnds = mutableListOf<Int>()
            val assignments = group.map { item ->
                val freeColumn = columnEnds.indexOfFirst { it <= item.startMinutes }
                val column = if (freeColumn >= 0) {
                    columnEnds[freeColumn] = item.endMinutes
                    freeColumn
                } else {
                    columnEnds.add(item.endMinutes)
                    columnEnds.lastIndex
                }
                item to column
            }
            val columns = columnEnds.size.coerceAtLeast(1)
            assignments.forEach { (item, column) ->
                result += PositionedPlan(
                    plan = item.plan,
                    dayIndex = dayIndex,
                    startMinutes = item.startMinutes,
                    endMinutes = item.endMinutes,
                    column = column,
                    columnWidthFraction = columns.toFloat(),
                )
            }
        }
    }
    return result
}

private fun minutesToLocalTime(minutes: Int): LocalTime =
    LocalTime.of((minutes / 60).coerceAtMost(23), minutes % 60)

private fun weekdayShort(date: LocalDate): String = when (date.dayOfWeek.value) {
    1 -> "一"
    2 -> "二"
    3 -> "三"
    4 -> "四"
    5 -> "五"
    6 -> "六"
    else -> "日"
}

private fun weekRangeLabel(start: LocalDate, end: LocalDate): String =
    if (start.month == end.month) {
        "${start.monthValue}月${start.dayOfMonth}日 — ${end.dayOfMonth}日"
    } else {
        "${start.monthValue}月${start.dayOfMonth}日 — ${end.monthValue}月${end.dayOfMonth}日"
    }

private fun quickAddSlot(now: LocalDateTime = LocalDateTime.now()): Triple<LocalDate, LocalTime, LocalTime> {
    val nextHalfHour = ((now.hour * 60 + now.minute) / 30 + 1) * 30
    val candidate = maxOf(nextHalfHour, VisibleStartHour * 60)
    val date = if (candidate > VisibleEndHour * 60 - 30) {
        now.toLocalDate().plusDays(1)
    } else {
        now.toLocalDate()
    }
    val minuteOfDay = if (date != now.toLocalDate()) {
        VisibleStartHour * 60
    } else {
        candidate
    }
    val start = minutesToLocalTime(minuteOfDay)
    val endMinutes = minOf(minuteOfDay + 60, VisibleEndHour * 60)
    return Triple(date, start, minutesToLocalTime(endMinutes))
}

@Preview(name = "有期 · 本周", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun WeekPlannerPreview() {
    val monday = mondayOf(LocalDate.now())
    fun samplePlan(
        id: Long,
        day: Long,
        startHour: Int,
        startMinute: Int,
        durationMinutes: Long,
        title: String,
        category: PlanCategory,
    ): Plan {
        val start = PlanTime.toMillis(monday.plusDays(day), LocalTime.of(startHour, startMinute))
        return Plan(
            id = id,
            title = title,
            notes = "",
            startAtMillis = start,
            endAtMillis = start + durationMinutes * 60_000L,
            leadMinutes = 30,
            category = category,
        )
    }

    val plans = listOf(
        samplePlan(1, 0, 9, 0, 90, "整理本周计划", PlanCategory.FOCUS),
        samplePlan(2, 1, 14, 0, 120, "产品评审", PlanCategory.WORK),
        samplePlan(3, 3, 18, 30, 60, "夜跑五公里", PlanCategory.SPORT),
        samplePlan(4, 5, 10, 0, 180, "阅读与写作", PlanCategory.STUDY),
    )

    PlanNextTheme {
        WeekPlannerScreen(
            plans = plans,
            weekStart = monday,
            permissions = PermissionSnapshot(true, true),
            onPreviousWeek = {},
            onNextWeek = {},
            onCurrentWeek = {},
            onOpenSlot = { _, _, _ -> },
            onEditPlan = {},
            onRequestNotifications = {},
            onOpenExactAlarmSettings = {},
        )
    }
}

