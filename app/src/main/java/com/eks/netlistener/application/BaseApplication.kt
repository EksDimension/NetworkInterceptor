package com.eks.netlistener.application

import android.app.Application
import android.content.Context
import com.eks.networkinterceptor.NetworkInterceptManager
import me.weishu.reflection.Reflection

/**
 * Created by Riggs on 2019/3/4
 */
class BaseApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        NetworkInterceptManager.init(this)
    }

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        Reflection.unseal(base)
    }
}