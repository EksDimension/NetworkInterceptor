package com.eks.networkinterceptor

import android.content.Context
import android.content.Context.CONNECTIVITY_SERVICE
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import com.eks.networkinterceptor.thread.ThreadPoolManager
import com.eks.networkinterceptor.type.WifiAvailability
import com.zhongruiandroid.timedown.PollingCheck
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method


/**
 * Created by Riggs on 2020/1/13
 */
class WifiAvailabilityInterceptor(
    private val mWifiAvailability: WifiAvailabilityCallback
) {
    private var pollingCheck: PollingCheck? = null

    companion object {
        var INTERVAL_TIME_IMMED: Long = 500
        var INTERVAL_TIME_NORMAL: Long = 5000
    }

    //    private var disposable: Disposable? = null
    private var checkIntervalTime = INTERVAL_TIME_IMMED

    fun startCheck() {
//        disposable?.dispose()//再中断一次之前的下游 确保同时只执行一次任务
//        Observable.create<Boolean> {
//            SystemClock.sleep(checkIntervalTime)
//            val isWifiConnectAndUnavailable = isWifiConnectAndUnavailable()
//            it.onNext(isWifiConnectAndUnavailable)
//            it.onComplete()
//        }.subscribeOn(Schedulers.io())
//            .observeOn(AndroidSchedulers.mainThread()).subscribe(object : Observer<Boolean> {
//                override fun onComplete() {
//                    startCheck()//递归操作
//                }
//
//                override fun onSubscribe(d: Disposable) {
//                    disposable = d
//                }
//
//                override fun onNext(t: Boolean) {
//                    if (t) {
//                        checkIntervalTime = INTERVAL_TIME_IMMED
//                        mWifiAvailability.onChecked(
//                            WifiAvailability.CONNECTED_BUT_UNAVAILABLE,
//                            wifiNetwork
//                        )
//                    } else {
//                        checkIntervalTime = INTERVAL_TIME_NORMAL
//                        mWifiAvailability.onChecked(
//                            WifiAvailability.UNCONNECTED_OR_AVAILABLE,
//                            wifiNetwork
//                        )
//                    }
//                }
//
//                override fun onError(e: Throwable) {
//                    startCheck()//递归操作
//                }
//            })

        if (pollingCheck == null) pollingCheck = PollingCheck.get()
        pollingCheck?.startForMillis(checkIntervalTime, 0, object : PollingCheck.CheckCallback {
            override fun onCheck(checkCount: Int): Boolean {
                ThreadPoolManager.getInstance().execute {
                    val isWifiConnectAndUnavailable = isWifiConnectAndUnavailable()
                    ThreadPoolManager.getInstance().runOnUiThread {
                        if (isWifiConnectAndUnavailable) {
                            checkIntervalTime = INTERVAL_TIME_IMMED
                            mWifiAvailability.onChecked(
                                WifiAvailability.CONNECTED_BUT_UNAVAILABLE,
                                wifiNetwork
                            )
                        } else {
                            checkIntervalTime = INTERVAL_TIME_NORMAL
                            mWifiAvailability.onChecked(
                                WifiAvailability.UNCONNECTED_OR_AVAILABLE,
                                wifiNetwork
                            )
                        }
                        startCheck()//递归操作
                    }
                }
                return true
            }

            override fun onComplete() {
            }
        })
    }

    //连上联网wifi的network对象
    private var wifiNetwork: Network? = null

    private fun isWifiConnectAndUnavailable(): Boolean {
//        val nw: Network? = try {
        getCurrentNetwork()
//        } catch (e: Exception) {
//            null
//        }
        val cmgr =
            AppGlobals.getApplication()?.getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        val nc = cmgr.getNetworkCapabilities(wifiNetwork)
        //通过如下这个判断，可以达到预期的效果
        if (nc != null && !nc.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)) {
            return true//已连接，但无法上网。
        }
        return false//wifi根本没连 或者连接了而且正常使用中
    }

    private fun getCurrentNetwork() {
        val wifiManager =
            AppGlobals.getApplication()?.applicationContext?.getSystemService(Context.WIFI_SERVICE) as WifiManager
        var method: Method? = null
        var network: Network? = null
        try {
            method = wifiManager.javaClass.getMethod("getCurrentNetwork")
        } catch (e: NoSuchMethodException) {
            e.printStackTrace()
        }
        try {
            network = method?.invoke(wifiManager) as? Network
        } catch (e: IllegalAccessException) {
            e.printStackTrace()
        } catch (e: InvocationTargetException) {
            e.printStackTrace()
        }
        wifiNetwork = network
    }

    fun stopCheck() {
//        disposable?.dispose()//中断下游不再接收
        pollingCheck?.onDestroy()
        pollingCheck = null
    }

    interface WifiAvailabilityCallback {
        fun onChecked(
            wifiAvailability: WifiAvailability,
            wifiNetwork: Network?
        )
    }
}