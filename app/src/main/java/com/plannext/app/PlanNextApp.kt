package com.plannext.app

import android.app.Application
import com.plannext.app.data.PlanRepository

class PlanNextApp : Application() {
    override fun onCreate() {
        super.onCreate()
        PlanRepository.getInstance(this)
    }
}

