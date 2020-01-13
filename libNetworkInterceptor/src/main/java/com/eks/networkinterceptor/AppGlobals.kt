package com.eks.networkinterceptor

import android.app.Application

/**
 * Created by Riggs on 2020/1/13
 */
object AppGlobals {
    private var sApplication: Application? = null

    fun getApplication(): Application? {
        if (sApplication == null) {
            try {
                val method = Class.forName("android.app.ActivityThread")
                    .getDeclaredMethod("currentApplication")
                sApplication = method.invoke(null) as Application
            } catch (e: Exception) {
                e.printStackTrace()
            }

        }
        return sApplication
    }
}
