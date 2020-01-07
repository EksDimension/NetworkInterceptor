package com.eks.netlistener.application

import android.app.Application
import com.eks.networkinterceptor.NetworkInterceptManager

/**
 * Created by Riggs on 2019/3/4
 */
class BaseApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        NetworkInterceptManager.init(this)
    }
}