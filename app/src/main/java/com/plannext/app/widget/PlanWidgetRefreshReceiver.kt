package com.plannext.app.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class PlanWidgetRefreshReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        PlanWidgetProvider.refresh(context)
    }
}

