package com.plannext.app.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.plannext.app.DateRequest
import com.plannext.app.PermissionSnapshot
import com.plannext.app.alarm.PlanAlarmScheduler
import com.plannext.app.alarm.PlanNotifications
import com.plannext.app.data.Plan
import com.plannext.app.data.PlanCategory
import com.plannext.app.data.PlanRepository
import com.plannext.app.ui.theme.Accent
import com.plannext.app.ui.theme.AccentSoft
import com.plannext.app.ui.theme.CardWhite
import com.plannext.app.ui.theme.Cream
import com.plannext.app.ui.theme.Ink
import com.plannext.app.ui.theme.Line
import com.plannext.app.ui.theme.MutedInk
import com.plannext.app.ui.theme.Night
import com.plannext.app.ui.theme.PlanNextTheme
import com.plannext.app.ui.theme.Sage
import com.plannext.app.ui.theme.WarmWhite
import com.plannext.app.util.PlanTime
import com.plannext.app.widget.PlanWidgetProvider
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@Composable
fun PlanNextRoot(
    repository: PlanRepository,
    openDateRequest: DateRequest?,
    onDateRequestHandled: () -> Unit,
    permissions: PermissionSnapshot,
    onRequestNotifications: () -> Unit,
    onOpenExactAlarmSettings: () -> Unit,
) {
    val context = LocalContext.current
    val scheduler = remember { PlanAlarmScheduler(context) }
    val plans by repository.plans.collectAsState()
    var selectedDateValue by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }
    var editorTarget by remember { mutableStateOf<PlanEditorTarget?>(null) }
    var planToDelete by remember { mutableStateOf<Plan?>(null) }
    val selectedDate = runCatching { LocalDate.parse(selectedDateValue) }
        .getOrDefault(LocalDate.now())

    LaunchedEffect(openDateRequest?.nonce) {
        openDateRequest?.let {
            selectedDateValue = it.date.toString()
            onDateRequestHandled()
        }
    }

    if (editorTarget == null) {
        HomeScreen(
            plans = plans,
            selectedDate = selectedDate,
            permissions = permissions,
            onSelectDate = { selectedDateValue = it.toString() },
            onAddPlan = { editorTarget = PlanEditorTarget(null, selectedDate) },
            onEditPlan = { editorTarget = PlanEditorTarget(it, selectedDate) },
            onToggleComplete = { plan ->
                val completed = !plan.isCompleted
                repository.setCompleted(plan.id, completed)
                if (completed) {
                    scheduler.cancel(plan.id)
                    PlanNotifications.cancel(context, plan.id)
                } else {
                    scheduler.schedule(plan.copy(isCompleted = false))
                }
                PlanWidgetProvider.refresh(context)
            },
            onRequestNotifications = onRequestNotifications,
            onOpenExactAlarmSettings = onOpenExactAlarmSettings,
        )
    } else {
        val target = editorTarget ?: return
        PlanEditorScreen(
            target = target,
            onDismiss = { editorTarget = null },
            onDelete = { plan -> planToDelete = plan },
            onSave = { plan ->
                repository.upsert(plan)
                scheduler.cancel(plan.id)
                PlanNotifications.cancel(context, plan.id)
                scheduler.schedule(plan)
                PlanWidgetProvider.refresh(context)
                selectedDateValue = PlanTime.toDate(plan.startAtMillis).toString()
                editorTarget = null
            },
        )
    }

    planToDelete?.let { plan ->
        AlertDialog(
            onDismissRequest = { planToDelete = null },
            shape = RoundedCornerShape(28.dp),
            containerColor = CardWhite,
            title = { Text("删除这项计划？", color = Ink) },
            text = { Text("“${plan.title}”将被永久删除。", color = MutedInk) },
            confirmButton = {
                TextButton(
                    onClick = {
                        scheduler.cancel(plan.id)
                        PlanNotifications.cancel(context, plan.id)
                        repository.delete(plan.id)
                        PlanWidgetProvider.refresh(context)
                        planToDelete = null
                        editorTarget = null
                    },
                ) {
                    Text("删除", color = Accent)
                }
            },
            dismissButton = {
                TextButton(onClick = { planToDelete = null }) {
                    Text("取消", color = MutedInk)
                }
            },
        )
    }
}

@Composable
private fun HomeScreen(
    plans: List<Plan>,
    selectedDate: LocalDate,
    permissions: PermissionSnapshot,
    onSelectDate: (LocalDate) -> Unit,
    onAddPlan: () -> Unit,
    onEditPlan: (Plan) -> Unit,
    onToggleComplete: (Plan) -> Unit,
    onRequestNotifications: () -> Unit,
    onOpenExactAlarmSettings: () -> Unit,
) {
    val today = LocalDate.now()
    val stripStart = if (selectedDate.isAfter(today.plusDays(7))) {
        selectedDate.minusDays(6)
    } else {
        today
    }
    val dates = remember(stripStart) {
        (0L until 14L).map { stripStart.plusDays(it) }
    }
    val dayPlans = plans
        .filter { PlanTime.toDate(it.startAtMillis) == selectedDate }
        .sortedBy { it.startAtMillis }
    val activeCount = dayPlans.count { !it.isCompleted }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    0f to Cream,
                    0.48f to Color(0xFFFAF7F2),
                    1f to WarmWhite,
                ),
            ),
    ) {
        Box(
            modifier = Modifier
                .size(260.dp)
                .align(Alignment.TopEnd)
                .padding(top = 12.dp)
                .background(Color(0x12F06C4F), CircleShape),
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 126.dp),
        ) {
            item {
                Header(selectedDate = selectedDate)
            }
            item {
                DateStrip(
                    dates = dates,
                    selectedDate = selectedDate,
                    today = today,
                    onSelectDate = onSelectDate,
                )
            }
            if (!permissions.notificationsEnabled || !permissions.exactAlarmsEnabled) {
                item {
                    PermissionCard(
                        permissions = permissions,
                        onRequestNotifications = onRequestNotifications,
                        onOpenExactAlarmSettings = onOpenExactAlarmSettings,
                    )
                }
            }
            item {
                DaySummary(
                    date = selectedDate,
                    today = today,
                    count = activeCount,
                )
            }
            if (dayPlans.isEmpty()) {
                item {
                    EmptyDay(
                        date = selectedDate,
                        onAddPlan = onAddPlan,
                    )
                }
            } else {
                items(dayPlans, key = { it.id }) { plan ->
                    PlanCard(
                        plan = plan,
                        onClick = { onEditPlan(plan) },
                        onToggleComplete = { onToggleComplete(plan) },
                    )
                }
            }
        }

        AddPlanButton(
            onClick = onAddPlan,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 18.dp),
        )
    }
}

@Composable
private fun Header(selectedDate: LocalDate) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 22.dp, end = 22.dp, top = 16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(Accent, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "✓",
                    color = Color.White,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(Modifier.width(10.dp))
            Text(
                text = "近程",
                style = MaterialTheme.typography.titleLarge,
                color = Ink,
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = PlanTime.formatFullDate(selectedDate),
                style = MaterialTheme.typography.bodyMedium,
                color = MutedInk,
            )
        }

        Spacer(Modifier.height(27.dp))
        Text(
            text = "把未来，\n安排得刚刚好。",
            style = MaterialTheme.typography.displaySmall,
            color = Ink,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = "先想清楚要做什么，剩下的交给近程准时提醒。",
            style = MaterialTheme.typography.bodyMedium,
            color = MutedInk,
        )
    }
}

@Composable
private fun DateStrip(
    dates: List<LocalDate>,
    selectedDate: LocalDate,
    today: LocalDate,
    onSelectDate: (LocalDate) -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 22.dp, vertical = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        items(dates, key = { it.toEpochDay() }) { date ->
            val selected = date == selectedDate
            val background = if (selected) Ink else CardWhite.copy(alpha = 0.78f)
            val contentColor = if (selected) Color.White else Ink
            Surface(
                modifier = Modifier
                    .width(57.dp)
                    .height(77.dp)
                    .clickable { onSelectDate(date) },
                shape = RoundedCornerShape(20.dp),
                color = background,
                shadowElevation = if (selected) 7.dp else 0.dp,
                border = if (selected) null else androidx.compose.foundation.BorderStroke(
                    1.dp,
                    Line.copy(alpha = 0.8f),
                ),
            ) {
                Column(
                    modifier = Modifier.padding(vertical = 11.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = PlanTime.weekday(date).removePrefix("周"),
                        color = contentColor.copy(alpha = 0.68f),
                        fontSize = 11.sp,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = date.dayOfMonth.toString(),
                        color = contentColor,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .size(4.dp)
                            .background(
                                when {
                                    date == today -> Accent
                                    selected -> Color.White.copy(alpha = 0.8f)
                                    else -> Color.Transparent
                                },
                                CircleShape,
                            ),
                    )
                }
            }
        }
    }
}

@Composable
private fun PermissionCard(
    permissions: PermissionSnapshot,
    onRequestNotifications: () -> Unit,
    onOpenExactAlarmSettings: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 22.dp),
        shape = RoundedCornerShape(26.dp),
        color = Night,
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "让提醒准时抵达",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(5.dp))
            Text(
                text = when {
                    !permissions.notificationsEnabled && !permissions.exactAlarmsEnabled ->
                        "开启通知与精确提醒，计划才会在设定时间出现。"
                    !permissions.notificationsEnabled ->
                        "通知权限尚未开启，计划提醒暂时不会显示。"
                    else ->
                        "精确提醒尚未开启，系统可能会稍微延迟提醒。"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.68f),
            )
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (!permissions.notificationsEnabled) {
                    PermissionAction(text = "开启通知", onClick = onRequestNotifications)
                }
                if (!permissions.exactAlarmsEnabled) {
                    PermissionAction(text = "精确提醒", onClick = onOpenExactAlarmSettings)
                }
            }
        }
    }
}

@Composable
private fun PermissionAction(text: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = CircleShape,
        color = AccentSoft,
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 15.dp, vertical = 8.dp),
            color = Accent,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

@Composable
private fun DaySummary(
    date: LocalDate,
    today: LocalDate,
    count: Int,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 22.dp, end = 22.dp, top = 27.dp, bottom = 13.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        Column {
            Text(
                text = PlanTime.relativeDay(date, today),
                style = MaterialTheme.typography.headlineMedium,
                color = Ink,
            )
            Text(
                text = "${PlanTime.weekday(date)} · ${PlanTime.formatDate(date)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MutedInk,
                modifier = Modifier.padding(top = 3.dp),
            )
        }
        Spacer(Modifier.weight(1f))
        Text(
            text = if (count == 0) "留白的一天" else "$count 项安排",
            style = MaterialTheme.typography.labelLarge,
            color = Accent,
        )
    }
}

@Composable
private fun EmptyDay(
    date: LocalDate,
    onAddPlan: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 22.dp)
            .clickable(onClick = onAddPlan),
        shape = RoundedCornerShape(28.dp),
        color = CardWhite.copy(alpha = 0.82f),
        border = androidx.compose.foundation.BorderStroke(1.dp, Line),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(62.dp)
                    .background(AccentSoft, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text("＋", color = Accent, fontSize = 28.sp, fontWeight = FontWeight.Light)
            }
            Spacer(Modifier.height(15.dp))
            Text(
                text = "${PlanTime.relativeDay(date)}还没有安排",
                style = MaterialTheme.typography.titleMedium,
                color = Ink,
            )
            Text(
                text = "给未来的自己留一件值得期待的事。",
                style = MaterialTheme.typography.bodyMedium,
                color = MutedInk,
                modifier = Modifier.padding(top = 5.dp),
            )
        }
    }
}

@Composable
private fun PlanCard(
    plan: Plan,
    onClick: () -> Unit,
    onToggleComplete: () -> Unit,
) {
    val categoryColors = categoryColors(plan.category)
    val isNow = System.currentTimeMillis() in plan.startAtMillis until plan.endAtMillis
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 22.dp, vertical = 5.dp)
            .clickable(onClick = onClick)
            .animateContentSize(),
        shape = RoundedCornerShape(26.dp),
        color = if (plan.isCompleted) CardWhite.copy(alpha = 0.56f) else CardWhite,
        shadowElevation = if (plan.isCompleted) 0.dp else 2.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, Line.copy(alpha = 0.72f)),
    ) {
        Row(
            modifier = Modifier.padding(17.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Column(
                modifier = Modifier.width(52.dp),
                horizontalAlignment = Alignment.Start,
            ) {
                Text(
                    text = PlanTime.formatTime(plan.startAtMillis),
                    color = if (plan.isCompleted) MutedInk else Ink,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = PlanTime.formatTime(plan.endAtMillis),
                    color = MutedInk.copy(alpha = 0.72f),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 2.dp),
                )
                Spacer(Modifier.height(11.dp))
                Box(
                    modifier = Modifier
                        .width(20.dp)
                        .height(3.dp)
                        .background(categoryColors.first, CircleShape),
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .alpha(if (plan.isCompleted) 0.55f else 1f),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = plan.title,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleMedium,
                        color = Ink,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        textDecoration = if (plan.isCompleted) {
                            TextDecoration.LineThrough
                        } else {
                            TextDecoration.None
                        },
                    )
                    if (isNow && !plan.isCompleted) {
                        Text(
                            text = "进行中",
                            color = Accent,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier
                                .background(AccentSoft, CircleShape)
                                .padding(horizontal = 8.dp, vertical = 3.dp),
                        )
                    }
                }

                if (plan.notes.isNotBlank()) {
                    Text(
                        text = plan.notes,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MutedInk,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 5.dp),
                    )
                }

                Spacer(Modifier.height(11.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = plan.category.label,
                        color = categoryColors.first,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .background(categoryColors.second, CircleShape)
                            .padding(horizontal = 9.dp, vertical = 4.dp),
                    )
                    Text(
                        text = "提前${leadLabel(plan.leadMinutes)}提醒",
                        color = MutedInk.copy(alpha = 0.8f),
                        fontSize = 11.sp,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            }

            Spacer(Modifier.width(10.dp))
            Box(
                modifier = Modifier
                    .size(31.dp)
                    .border(
                        width = 1.5.dp,
                        color = if (plan.isCompleted) Sage else Line,
                        shape = CircleShape,
                    )
                    .background(
                        if (plan.isCompleted) Sage else Color.Transparent,
                        CircleShape,
                    )
                    .clickable(onClick = onToggleComplete),
                contentAlignment = Alignment.Center,
            ) {
                if (plan.isCompleted) {
                    Text("✓", color = Color.White, fontSize = 15.sp)
                }
            }
        }
    }
}

@Composable
private fun AddPlanButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = CircleShape,
        color = Ink,
        shadowElevation = 12.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 22.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "＋",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Light,
            )
            Spacer(Modifier.width(7.dp))
            Text(
                text = "安排计划",
                color = Color.White,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

private fun leadLabel(minutes: Int): String = when (minutes) {
    15 -> "15 分钟"
    30 -> "30 分钟"
    60 -> "1 小时"
    90 -> "1.5 小时"
    120 -> "2 小时"
    else -> "$minutes 分钟"
}

private fun categoryColors(category: PlanCategory): Pair<Color, Color> = when (category) {
    PlanCategory.FOCUS -> Accent to AccentSoft
    PlanCategory.WORK -> Color(0xFF5E7894) to Color(0xFFEAF0F5)
    PlanCategory.STUDY -> Color(0xFF8B6F9C) to Color(0xFFF1EBF5)
    PlanCategory.LIFE -> Color(0xFFB57B45) to Color(0xFFF7EEE5)
    PlanCategory.SPORT -> Color(0xFF64896D) to Color(0xFFEAF2EB)
}

data class PlanEditorTarget(
    val plan: Plan?,
    val initialDate: LocalDate,
)

internal fun defaultPlanTimes(
    date: LocalDate,
    now: LocalDateTime = LocalDateTime.now(),
): Pair<LocalTime, LocalTime> {
    val start = if (date == now.toLocalDate()) {
        val candidate = now.plusMinutes(30)
        if (candidate.toLocalDate() != date) {
            LocalTime.of(23, 45)
        } else {
            val roundedMinute = ((candidate.minute + 14) / 15) * 15
            if (roundedMinute == 60) {
                candidate.plusHours(1).withMinute(0).withSecond(0).withNano(0).toLocalTime()
            } else {
                candidate.withMinute(roundedMinute).withSecond(0).withNano(0).toLocalTime()
            }
        }
    } else {
        LocalTime.of(9, 0)
    }
    val proposedEnd = start.plusHours(1)
    val end = if (proposedEnd.isAfter(start)) proposedEnd else LocalTime.of(23, 59)
    return start to end
}

@Preview(name = "近程首页", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun HomeScreenPreview() {
    val date = LocalDate.now().plusDays(1)
    val firstStart = PlanTime.toMillis(date, LocalTime.of(9, 30))
    val secondStart = PlanTime.toMillis(date, LocalTime.of(14, 0))
    val plans = listOf(
        Plan(
            id = 1,
            title = "整理下周的产品方案",
            notes = "带上电脑和上一轮讨论记录",
            startAtMillis = firstStart,
            endAtMillis = firstStart + 90 * 60_000L,
            leadMinutes = 30,
            category = PlanCategory.WORK,
        ),
        Plan(
            id = 2,
            title = "公园慢跑",
            notes = "轻松跑 5 公里",
            startAtMillis = secondStart,
            endAtMillis = secondStart + 60 * 60_000L,
            leadMinutes = 60,
            category = PlanCategory.SPORT,
        ),
    )
    PlanNextTheme {
        HomeScreen(
            plans = plans,
            selectedDate = date,
            permissions = PermissionSnapshot(true, true),
            onSelectDate = {},
            onAddPlan = {},
            onEditPlan = {},
            onToggleComplete = {},
            onRequestNotifications = {},
            onOpenExactAlarmSettings = {},
        )
    }
}

