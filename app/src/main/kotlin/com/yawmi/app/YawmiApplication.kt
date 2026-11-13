package com.yawmi.app

import android.app.Application
import com.yawmi.app.di.AppContainer

class YawmiApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.seedDefaults()
    }
}
