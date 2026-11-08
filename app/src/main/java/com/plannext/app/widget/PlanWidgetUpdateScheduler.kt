package com.plannext.app.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.plannext.app.data.PlanRepository

object PlanWidgetUpdateScheduler {
    private const val ACTION_REFRESH_WIDGET = "com.plannext.app.action.REFRESH_WIDGET"

    fun scheduleNext(context: Context) {
        val appContext = context.applicationContext
        val alarmManager = appContext.getSystemService(AlarmManager::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            appContext,
            8_101,
            Intent(appContext, PlanWidgetRefreshReceiver::class.java)
                .setAction(ACTION_REFRESH_WIDGET),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        alarmManager.cancel(pendingIntent)

        val now = System.currentTimeMillis()
        val nextBoundary = PlanRepository.getInstance(appContext).snapshot()
            .filterNot { it.isCompleted }
            .flatMap { listOf(it.startAtMillis, it.endAtMillis) }
            .filter { it > now }
            .minOrNull()
            ?: return

        alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            nextBoundary.coerceAtLeast(now + 2_000L),
            pendingIntent,
        )
    }
}

