package com.plannext.app.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import com.plannext.app.data.Plan
import com.plannext.app.data.PlanRepository

class PlanAlarmScheduler(context: Context) {
    private val appContext = context.applicationContext
    private val alarmManager = appContext.getSystemService(AlarmManager::class.java)

    fun schedule(plan: Plan) {
        cancel(plan.id)
        if (plan.isCompleted || plan.endAtMillis <= System.currentTimeMillis()) return

        val desiredTrigger = plan.startAtMillis - plan.leadMinutes * 60_000L
        if (desiredTrigger <= System.currentTimeMillis()) return
        val triggerAt = desiredTrigger
        val operation = alarmPendingIntent(plan.id)

        runCatching {
            if (canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAt,
                    operation,
                )
            } else {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAt,
                    operation,
                )
            }
        }
    }

    fun cancel(planId: Long) {
        alarmManager.cancel(alarmPendingIntent(planId))
    }

    fun scheduleAll(plans: List<Plan> = PlanRepository.getInstance(appContext).snapshot()) {
        plans.filterNot { it.isCompleted }.forEach(::schedule)
    }

    fun canScheduleExactAlarms(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()

    private fun alarmPendingIntent(planId: Long): PendingIntent {
        val intent = Intent(appContext, PlanAlarmReceiver::class.java)
            .setAction(PlanAlarmReceiver.ACTION_PLAN_ALARM)
            .setData(Uri.parse("plannext://alarm/$planId"))
            .putExtra(PlanAlarmReceiver.EXTRA_PLAN_ID, planId)
        return PendingIntent.getBroadcast(
            appContext,
            requestCodeFor(planId),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun requestCodeFor(planId: Long): Int =
        (planId xor (planId ushr 32)).toInt()
}

