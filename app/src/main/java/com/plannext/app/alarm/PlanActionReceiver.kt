package com.plannext.app.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.plannext.app.data.PlanRepository
import com.plannext.app.widget.PlanWidgetProvider

class PlanActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_COMPLETE) return
        val planId = intent.getLongExtra(EXTRA_PLAN_ID, -1L)
        if (planId == -1L) return

        PlanRepository.getInstance(context).setCompleted(planId)
        PlanAlarmScheduler(context).cancel(planId)
        PlanNotifications.cancel(context, planId)
        PlanWidgetProvider.refresh(context)
    }

    companion object {
        const val ACTION_COMPLETE = "com.plannext.app.action.COMPLETE_PLAN"
        const val EXTRA_PLAN_ID = "plan_id"
    }
}

