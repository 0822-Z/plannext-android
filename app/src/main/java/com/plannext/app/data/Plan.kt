package com.plannext.app.data

enum class PlanCategory(val label: String) {
    FOCUS("专注"),
    WORK("工作"),
    STUDY("学习"),
    LIFE("生活"),
    SPORT("运动"),
    ;

    companion object {
        fun fromStored(value: String?): PlanCategory =
            entries.firstOrNull { it.name == value } ?: FOCUS
    }
}

data class Plan(
    val id: Long,
    val title: String,
    val notes: String,
    val startAtMillis: Long,
    val endAtMillis: Long,
    val leadMinutes: Int,
    val category: PlanCategory,
    val isCompleted: Boolean = false,
)

