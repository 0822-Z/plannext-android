package com.plannext.app.ui

import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.platform.LocalContext
import com.plannext.app.DateRequest
import com.plannext.app.PermissionSnapshot
import com.plannext.app.alarm.PlanAlarmScheduler
import com.plannext.app.alarm.PlanNotifications
import com.plannext.app.data.Plan
import com.plannext.app.data.PlanRepository
import com.plannext.app.ui.theme.Accent
import com.plannext.app.ui.theme.CardWhite
import com.plannext.app.ui.theme.Ink
import com.plannext.app.ui.theme.MutedInk
import com.plannext.app.util.PlanTime
import com.plannext.app.widget.PlanWidgetProvider
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.TemporalAdjusters

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
    var weekStartValue by rememberSaveable {
        mutableStateOf(mondayOf(LocalDate.now()).toString())
    }
    var editorTarget by remember { mutableStateOf<PlanEditorTarget?>(null) }
    var planToDelete by remember { mutableStateOf<Plan?>(null) }
    val weekStart = runCatching { LocalDate.parse(weekStartValue) }
        .getOrDefault(mondayOf(LocalDate.now()))

    LaunchedEffect(openDateRequest?.nonce) {
        openDateRequest?.let {
            weekStartValue = mondayOf(it.date).toString()
            onDateRequestHandled()
        }
    }

    if (editorTarget == null) {
        WeekPlannerScreen(
            plans = plans,
            weekStart = weekStart,
            permissions = permissions,
            onPreviousWeek = { weekStartValue = weekStart.minusWeeks(1).toString() },
            onNextWeek = { weekStartValue = weekStart.plusWeeks(1).toString() },
            onCurrentWeek = { weekStartValue = mondayOf(LocalDate.now()).toString() },
            onOpenSlot = { date, startTime, endTime ->
                editorTarget = PlanEditorTarget(
                    plan = null,
                    initialDate = date,
                    initialStartTime = startTime,
                    initialEndTime = endTime,
                )
            },
            onEditPlan = { plan ->
                editorTarget = PlanEditorTarget(
                    plan = plan,
                    initialDate = PlanTime.toDate(plan.startAtMillis),
                )
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
                if (!plan.isCompleted) {
                    scheduler.schedule(plan)
                }
                PlanWidgetProvider.refresh(context)
                weekStartValue = mondayOf(PlanTime.toDate(plan.startAtMillis)).toString()
                editorTarget = null
            },
        )
    }

    planToDelete?.let { plan ->
        AlertDialog(
            onDismissRequest = { planToDelete = null },
            containerColor = CardWhite,
            title = { Text("删除这项安排？", color = Ink) },
            text = { Text("“${plan.title}”将从有期中移除。", color = MutedInk) },
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

internal fun mondayOf(date: LocalDate): LocalDate =
    date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

internal fun defaultPlanTimes(
    date: LocalDate,
    now: LocalDateTime = LocalDateTime.now(),
): Pair<LocalTime, LocalTime> {
    if (date != now.toLocalDate()) {
        return LocalTime.of(9, 0) to LocalTime.of(10, 0)
    }

    val currentMinutes = now.hour * 60 + now.minute
    val roundedMinutes = ((currentMinutes / 30) + 1) * 30
    val start = if (roundedMinutes >= 24 * 60) {
        LocalTime.of(23, 30)
    } else {
        LocalTime.of(roundedMinutes / 60, roundedMinutes % 60)
    }
    val proposedEnd = start.plusHours(1)
    val end = if (proposedEnd.isAfter(start)) proposedEnd else LocalTime.of(23, 59)
    return start to end
}

data class PlanEditorTarget(
    val plan: Plan?,
    val initialDate: LocalDate,
    val initialStartTime: LocalTime? = null,
    val initialEndTime: LocalTime? = null,
)

