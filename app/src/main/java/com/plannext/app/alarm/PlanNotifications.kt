package com.plannext.app.alarm

import android.app.NotificationManager
import android.content.Context

object PlanNotifications {
    fun notificationId(planId: Long): Int =
        (planId xor (planId ushr 32)).toInt() and Int.MAX_VALUE

    fun cancel(context: Context, planId: Long) {
        context.getSystemService(NotificationManager::class.java)
            .cancel(notificationId(planId))
    }
}

