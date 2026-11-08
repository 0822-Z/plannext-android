package com.plannext.app.alarm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import com.plannext.app.MainActivity
import com.plannext.app.R
import com.plannext.app.data.Plan
import com.plannext.app.data.PlanRepository
import com.plannext.app.util.PlanTime
import com.plannext.app.widget.PlanWidgetProvider

class PlanAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_PLAN_ALARM) return
        val planId = intent.getLongExtra(EXTRA_PLAN_ID, -1L)
        if (planId == -1L) return

        val repository = PlanRepository.getInstance(context)
        val plan = repository.getPlan(planId) ?: return
        if (plan.isCompleted || plan.endAtMillis <= System.currentTimeMillis()) return

        createChannel(context)
        showNotification(context, plan)
        PlanWidgetProvider.refresh(context)
    }

    private fun showNotification(context: Context, plan: Plan) {
        val notificationManager = context.getSystemService(NotificationManager::class.java)
        val date = PlanTime.toDate(plan.startAtMillis)
        val title = when {
            plan.startAtMillis <= System.currentTimeMillis() -> "现在开始 · ${plan.title}"
            else -> "${PlanTime.relativeRemainingLabel(plan.startAtMillis)} · ${plan.title}"
        }
        val timeRange = "${PlanTime.formatTime(plan.startAtMillis)}–${PlanTime.formatTime(plan.endAtMillis)}"
        val content = "${PlanTime.relativeDay(date)} $timeRange · ${plan.category.label}"

        val openIntent = Intent(context, MainActivity::class.java)
            .putExtra(MainActivity.EXTRA_OPEN_DATE, date.toString())
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val openPendingIntent = PendingIntent.getActivity(
            context,
            requestCode(plan.id, 1),
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val completeIntent = Intent(context, PlanActionReceiver::class.java)
            .setAction(PlanActionReceiver.ACTION_COMPLETE)
            .setData(Uri.parse("plannext://complete/${plan.id}"))
            .putExtra(PlanActionReceiver.EXTRA_PLAN_ID, plan.id)
        val completePendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode(plan.id, 2),
            completeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_plannext)
            .setColor(Color.rgb(240, 108, 79))
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(Notification.BigTextStyle().bigText("$content\n${plan.notes}".trim()))
            .setContentIntent(openPendingIntent)
            .setAutoCancel(true)
            .setCategory(Notification.CATEGORY_REMINDER)
            .setVisibility(Notification.VISIBILITY_PRIVATE)
            .setPriority(Notification.PRIORITY_HIGH)
            .setWhen(plan.startAtMillis)
            .setShowWhen(true)
            .addAction(Notification.Action.Builder(null, "完成", completePendingIntent).build())
            .build()

        notificationManager.notify(PlanNotifications.notificationId(plan.id), notification)
    }

    private fun createChannel(context: Context) {
        val notificationManager = context.getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            "计划提醒",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "在计划开始前准时提醒你"
            enableVibration(true)
            enableLights(true)
            lightColor = Color.rgb(240, 108, 79)
        }
        notificationManager.createNotificationChannel(channel)
    }

    private fun requestCode(planId: Long, salt: Int): Int =
        ((planId xor (planId ushr 32)).toInt() * 31) + salt

    companion object {
        const val ACTION_PLAN_ALARM = "com.plannext.app.action.PLAN_ALARM"
        const val EXTRA_PLAN_ID = "plan_id"
        const val CHANNEL_ID = "plan_reminders_v1"
    }
}

