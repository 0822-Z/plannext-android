package com.plannext.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.RemoteViews
import com.plannext.app.MainActivity
import com.plannext.app.R
import com.plannext.app.data.Plan
import com.plannext.app.data.PlanRepository
import com.plannext.app.util.PlanTime
import java.time.LocalDate

class PlanWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        appWidgetIds.forEach { widgetId ->
            appWidgetManager.updateAppWidget(widgetId, buildRemoteViews(context))
        }
        PlanWidgetUpdateScheduler.scheduleNext(context)
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: android.os.Bundle,
    ) {
        appWidgetManager.updateAppWidget(appWidgetId, buildRemoteViews(context))
    }

    private fun buildRemoteViews(context: Context): RemoteViews {
        val plans = PlanRepository.getInstance(context).snapshot()
        val now = System.currentTimeMillis()
        val nextPlan = plans
            .asSequence()
            .filterNot { it.isCompleted }
            .filter { it.endAtMillis > now }
            .minByOrNull { it.startAtMillis }

        val date = nextPlan?.let { PlanTime.toDate(it.startAtMillis) } ?: LocalDate.now()
        val openDateIntent = Intent(context, MainActivity::class.java)
            .setData(Uri.parse("plannext://widget/$date"))
            .putExtra(MainActivity.EXTRA_OPEN_DATE, date.toString())
        val openDatePendingIntent = PendingIntent.getActivity(
            context,
            date.toEpochDay().toInt(),
            openDateIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        return RemoteViews(context.packageName, R.layout.plannext_widget).apply {
            setOnClickPendingIntent(R.id.widget_root, openDatePendingIntent)
            if (nextPlan == null) {
                setTextViewText(R.id.widget_status, "NEXT · 下一项")
                setTextViewText(R.id.widget_title, "今天留给重要的事")
                setTextViewText(
                    R.id.widget_time,
                    context.getString(R.string.widget_default_action),
                )
                setTextViewText(R.id.widget_meta, "你的下一次计划会出现在这里")
            } else {
                bindPlan(this, plans, nextPlan, now)
            }
        }
    }

    private fun bindPlan(views: RemoteViews, plans: List<Plan>, plan: Plan, now: Long) {
        val date = PlanTime.toDate(plan.startAtMillis)
        val dayPlans = plans.filter {
            !it.isCompleted && PlanTime.toDate(it.startAtMillis) == date
        }
        val state = when {
            plan.startAtMillis <= now -> "进行中"
            else -> PlanTime.relativeRemainingLabel(plan.startAtMillis, now)
        }
        val dayLabel = PlanTime.relativeDay(date)
        val status = if (dayPlans.size > 1) {
            "$dayLabel · $state · 共${dayPlans.size}项"
        } else {
            "$dayLabel · $state"
        }
        val time = "${PlanTime.formatTime(plan.startAtMillis)}–${PlanTime.formatTime(plan.endAtMillis)}"
        val meta = "${plan.category.label} · ${PlanTime.durationLabel(plan.startAtMillis, plan.endAtMillis)}"

        views.setTextViewText(R.id.widget_status, status)
        views.setTextViewText(R.id.widget_title, plan.title)
        views.setTextViewText(R.id.widget_time, time)
        views.setTextViewText(R.id.widget_meta, meta)
    }

    companion object {
        fun refresh(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val component = ComponentName(context, PlanWidgetProvider::class.java)
            val ids = manager.getAppWidgetIds(component)
            if (ids.isEmpty()) return
            ids.forEach { id ->
                val plans = PlanRepository.getInstance(context).snapshot()
                val now = System.currentTimeMillis()
                val nextPlan = plans
                    .asSequence()
                    .filterNot { it.isCompleted }
                    .filter { it.endAtMillis > now }
                    .minByOrNull { it.startAtMillis }
                val date = nextPlan?.let { PlanTime.toDate(it.startAtMillis) } ?: LocalDate.now()
                val intent = Intent(context, MainActivity::class.java)
                    .setData(Uri.parse("plannext://widget/$date"))
                    .putExtra(MainActivity.EXTRA_OPEN_DATE, date.toString())
                val pendingIntent = PendingIntent.getActivity(
                    context,
                    date.toEpochDay().toInt(),
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )
                val views = PlanWidgetProvider().buildRemoteViews(context)
                    .apply { setOnClickPendingIntent(R.id.widget_root, pendingIntent) }
                manager.updateAppWidget(id, views)
            }
            PlanWidgetUpdateScheduler.scheduleNext(context)
        }
    }
}

