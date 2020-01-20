package com.eks.netlistener.application

import android.app.Application

/**
 * Created by Riggs on 2019/3/4
 */
class BaseApplication : Application() {
    override fun onCreate() {
        super.onCreate()
//        NetworkInterceptManager.setCustomServers(arrayOf(SocketAddressForTesting("www.google.com",80),SocketAddressForTesting("www.bilibili.com",80)))
    }
}