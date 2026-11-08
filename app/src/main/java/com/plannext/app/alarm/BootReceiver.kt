package com.plannext.app.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.plannext.app.data.PlanRepository
import com.plannext.app.widget.PlanWidgetProvider

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val supportedActions = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
        )
        if (intent.action !in supportedActions) return

        val repository = PlanRepository.getInstance(context)
        PlanAlarmScheduler(context).scheduleAll(repository.snapshot())
        PlanWidgetProvider.refresh(context)
    }
}

