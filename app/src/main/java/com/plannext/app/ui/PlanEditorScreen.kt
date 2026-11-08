package com.plannext.app.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.plannext.app.data.Plan
import com.plannext.app.data.PlanCategory
import com.plannext.app.ui.theme.Accent
import com.plannext.app.ui.theme.AccentSoft
import com.plannext.app.ui.theme.CardWhite
import com.plannext.app.ui.theme.Cream
import com.plannext.app.ui.theme.Ink
import com.plannext.app.ui.theme.Line
import com.plannext.app.ui.theme.MutedInk
import com.plannext.app.ui.theme.WarmWhite
import com.plannext.app.util.PlanTime
import java.time.LocalDate
import java.time.LocalTime

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PlanEditorScreen(
    target: PlanEditorTarget,
    onDismiss: () -> Unit,
    onSave: (Plan) -> Unit,
    onDelete: (Plan) -> Unit,
) {
    val context = LocalContext.current
    val existing = target.plan
    val initialDate = existing?.let { PlanTime.toDate(it.startAtMillis) } ?: target.initialDate
    val defaultTimes = remember(initialDate) {
        existing?.let {
            PlanTime.toTime(it.startAtMillis) to PlanTime.toTime(it.endAtMillis)
        } ?: defaultPlanTimes(initialDate)
    }

    var dateValue by rememberSaveable { mutableStateOf(initialDate.toString()) }
    var startHour by rememberSaveable { mutableIntStateOf(defaultTimes.first.hour) }
    var startMinute by rememberSaveable { mutableIntStateOf(defaultTimes.first.minute) }
    var endHour by rememberSaveable { mutableIntStateOf(defaultTimes.second.hour) }
    var endMinute by rememberSaveable { mutableIntStateOf(defaultTimes.second.minute) }
    var title by rememberSaveable { mutableStateOf(existing?.title.orEmpty()) }
    var notes by rememberSaveable { mutableStateOf(existing?.notes.orEmpty()) }
    var category by rememberSaveable {
        mutableStateOf(existing?.category ?: PlanCategory.FOCUS)
    }
    var leadMinutes by rememberSaveable {
        mutableIntStateOf(existing?.leadMinutes ?: -1)
    }
    var validationMessage by remember { mutableStateOf<String?>(null) }

    val date = runCatching { LocalDate.parse(dateValue) }.getOrDefault(initialDate)
    val startTime = LocalTime.of(startHour, startMinute)
    val endTime = LocalTime.of(endHour, endMinute)
    val startAtMillis = PlanTime.toMillis(date, startTime)
    val endAtMillis = PlanTime.toMillis(date, endTime)

    BackHandler(onBack = onDismiss)

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Cream,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding(),
        ) {
            EditorTopBar(
                isEditing = existing != null,
                onDismiss = onDismiss,
                onSave = {
                    validationMessage = validatePlan(
                        title = title,
                        startAtMillis = startAtMillis,
                        endAtMillis = endAtMillis,
                        leadMinutes = leadMinutes,
                    )
                    if (validationMessage == null) {
                        onSave(
                            Plan(
                                id = existing?.id ?: System.currentTimeMillis(),
                                title = title.trim(),
                                notes = notes.trim(),
                                startAtMillis = startAtMillis,
                                endAtMillis = endAtMillis,
                                leadMinutes = leadMinutes,
                                category = category,
                                isCompleted = existing?.isCompleted ?: false,
                            ),
                        )
                    }
                },
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 22.dp),
            ) {
                Text(
                    text = if (existing == null) "下一件事，\n从什么时候开始？" else "调整这次安排",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Ink,
                    modifier = Modifier.padding(top = 22.dp),
                )
                Text(
                    text = "时间与提醒方式都确定后，计划才会被保存。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MutedInk,
                    modifier = Modifier.padding(top = 7.dp),
                )

                SectionLabel("做什么", Modifier.padding(top = 29.dp))
                TextField(
                    value = title,
                    onValueChange = {
                        title = it.take(60)
                        validationMessage = null
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 9.dp),
                    placeholder = {
                        Text("例如：整理下周项目计划", color = MutedInk.copy(alpha = 0.7f))
                    },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.titleMedium,
                    shape = RoundedCornerShape(22.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    colors = editorFieldColors(),
                )

                SectionLabel("日期与时间", Modifier.padding(top = 24.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 9.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    SelectionCard(
                        label = "日期",
                        value = "${PlanTime.formatDate(date)} ${PlanTime.weekday(date)}",
                        modifier = Modifier.weight(1f),
                        onClick = {
                            DatePickerDialog(
                                context,
                                { _, year, month, day ->
                                    dateValue = LocalDate.of(year, month + 1, day).toString()
                                    validationMessage = null
                                },
                                date.year,
                                date.monthValue - 1,
                                date.dayOfMonth,
                            ).apply {
                                datePicker.minDate = PlanTime.toMillis(LocalDate.now(), LocalTime.MIN)
                            }.show()
                        },
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    SelectionCard(
                        label = "开始",
                        value = startTime.toString(),
                        modifier = Modifier.weight(1f),
                        onClick = {
                            TimePickerDialog(
                                context,
                                { _, hour, minute ->
                                    startHour = hour
                                    startMinute = minute
                                    val picked = LocalTime.of(hour, minute)
                                    if (!endTime.isAfter(picked)) {
                                        endHour = picked.plusMinutes(60).hour
                                        endMinute = picked.plusMinutes(60).minute
                                    }
                                    validationMessage = null
                                },
                                startHour,
                                startMinute,
                                true,
                            ).show()
                        },
                    )
                    SelectionCard(
                        label = "结束",
                        value = endTime.toString(),
                        modifier = Modifier.weight(1f),
                        onClick = {
                            TimePickerDialog(
                                context,
                                { _, hour, minute ->
                                    endHour = hour
                                    endMinute = minute
                                    validationMessage = null
                                },
                                endHour,
                                endMinute,
                                true,
                            ).show()
                        },
                    )
                }

                SectionLabel("分类", Modifier.padding(top = 24.dp))
                FlowRow(
                    modifier = Modifier.padding(top = 9.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    PlanCategory.entries.forEach { item ->
                        ChoiceChip(
                            text = item.label,
                            selected = category == item,
                            onClick = { category = item },
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 25.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SectionLabel("提前提醒", Modifier.weight(1f))
                    Text(
                        text = "必选",
                        color = Accent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .background(AccentSoft, CircleShape)
                            .padding(horizontal = 9.dp, vertical = 4.dp),
                    )
                }
                Text(
                    text = "通知会在计划开始前的指定时间发送。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MutedInk,
                    modifier = Modifier.padding(top = 3.dp),
                )
                FlowRow(
                    modifier = Modifier.padding(top = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    reminderOptions.forEach { option ->
                        ChoiceChip(
                            text = option.label,
                            selected = leadMinutes == option.minutes,
                            onClick = {
                                leadMinutes = option.minutes
                                validationMessage = null
                            },
                        )
                    }
                }

                if (leadMinutes in reminderOptions.map { it.minutes }) {
                    val triggerAt = startAtMillis - leadMinutes * 60_000L
                    if (triggerAt > System.currentTimeMillis()) {
                        Text(
                            text = "将在 ${PlanTime.formatDate(PlanTime.toDate(triggerAt))} " +
                                "${PlanTime.formatTime(triggerAt)} 提醒你",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MutedInk,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp)
                                .background(WarmWhite, RoundedCornerShape(12.dp))
                                .padding(horizontal = 12.dp, vertical = 9.dp),
                        )
                    }
                }

                SectionLabel("备注（可选）", Modifier.padding(top = 25.dp))
                TextField(
                    value = notes,
                    onValueChange = { notes = it.take(240) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 9.dp),
                    placeholder = {
                        Text("例如：带上电脑和会议资料", color = MutedInk.copy(alpha = 0.7f))
                    },
                    minLines = 3,
                    maxLines = 5,
                    textStyle = MaterialTheme.typography.bodyLarge,
                    shape = RoundedCornerShape(22.dp),
                    colors = editorFieldColors(),
                )

                validationMessage?.let { message ->
                    Text(
                        text = message,
                        color = Accent,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp)
                            .background(AccentSoft, RoundedCornerShape(14.dp))
                            .padding(13.dp),
                    )
                }

                if (existing != null) {
                    Text(
                        text = "删除这项计划",
                        color = Accent,
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 25.dp)
                            .clickable { onDelete(existing) }
                            .padding(vertical = 13.dp),
                    )
                }
                Spacer(Modifier.height(36.dp))
            }
        }
    }
}

@Composable
private fun EditorTopBar(
    isEditing: Boolean,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "‹  返回",
            color = Ink,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier
                .clickable(onClick = onDismiss)
                .padding(horizontal = 8.dp, vertical = 11.dp),
        )
        Spacer(Modifier.weight(1f))
        Text(
            text = if (isEditing) "编辑计划" else "新计划",
            color = MutedInk,
            style = MaterialTheme.typography.labelLarge,
        )
        Spacer(Modifier.weight(1f))
        Surface(
            modifier = Modifier.clickable(onClick = onSave),
            color = Ink,
            shape = CircleShape,
        ) {
            Text(
                text = "保存",
                color = Color.White,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
            )
        }
    }
}

@Composable
private fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier,
        color = Ink,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
    )
}

@Composable
private fun SelectionCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = CardWhite,
        border = BorderStroke(1.dp, Line),
    ) {
        Column(modifier = Modifier.padding(horizontal = 15.dp, vertical = 13.dp)) {
            Text(
                text = label,
                color = MutedInk,
                fontSize = 11.sp,
            )
            Text(
                text = value,
                color = Ink,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 3.dp),
            )
        }
    }
}

@Composable
private fun ChoiceChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = CircleShape,
        color = if (selected) Ink else CardWhite,
        border = if (selected) null else BorderStroke(1.dp, Line),
    ) {
        Text(
            text = text,
            color = if (selected) Color.White else Ink,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 15.dp, vertical = 9.dp),
        )
    }
}

@Composable
private fun editorFieldColors() = TextFieldDefaults.colors(
    focusedContainerColor = CardWhite,
    unfocusedContainerColor = CardWhite,
    focusedIndicatorColor = Color.Transparent,
    unfocusedIndicatorColor = Color.Transparent,
    cursorColor = Accent,
    focusedPlaceholderColor = MutedInk.copy(alpha = 0.7f),
    unfocusedPlaceholderColor = MutedInk.copy(alpha = 0.7f),
)

private fun validatePlan(
    title: String,
    startAtMillis: Long,
    endAtMillis: Long,
    leadMinutes: Int,
): String? {
    val now = System.currentTimeMillis()
    return when {
        title.isBlank() -> "请先写下这次计划要做什么。"
        endAtMillis <= startAtMillis -> "结束时间需要晚于开始时间。"
        startAtMillis <= now + 30_000L -> "开始时间已过去，请选择未来的时间。"
        leadMinutes !in reminderOptions.map { it.minutes } -> "请选择提前提醒时间，这是必选项。"
        startAtMillis - leadMinutes * 60_000L < now - 30_000L ->
            "距离计划开始不足${leadMinutesLabel(leadMinutes)}，请选择更短的提前时间。"
        else -> null
    }
}

private fun leadMinutesLabel(minutes: Int): String = when (minutes) {
    60 -> "1小时"
    90 -> "1.5小时"
    120 -> "2小时"
    else -> "${minutes}分钟"
}

private data class ReminderOption(
    val label: String,
    val minutes: Int,
)

private val reminderOptions = listOf(
    ReminderOption("提前 15 分钟", 15),
    ReminderOption("提前 30 分钟", 30),
    ReminderOption("提前 1 小时", 60),
    ReminderOption("提前 1.5 小时", 90),
    ReminderOption("提前 2 小时", 120),
)

