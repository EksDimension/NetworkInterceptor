package com.eks.networkinterceptor

import android.os.SystemClock
import com.eks.networkinterceptor.type.DataAvailability
import io.reactivex.Observable
import io.reactivex.Observer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader


/**
 * 检测网络是否真正连通
 * Created by Riggs on 1/10/2020
 */
class NetworkDataAvailabilityInterceptor(private val mAvailabilityCallback: DataAvailabilityCallback) {

    private var HANDLER_MSG_INTERVAL = 0
    private var INTERVAL_DURATION: Long = 5000


    private var disposable: Disposable? = null
//    private var counter = 0

//    private var mHandler = MHandler(this)


    fun startCheck() {
        disposable?.dispose()//再中断一次之前的下游 确保同时只执行一次任务
//        mHandler.removeCallbacksAndMessages(null)
//        mHandler.sendEmptyMessageDelayed(HANDLER_MSG_INTERVAL, INTERVAL_DURATION)
        Observable.create<Boolean> {
            SystemClock.sleep(5000)
            val networkOnline = isNetworkOnline()
            it.onNext(networkOnline)
            it.onComplete()
        }.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread()).subscribe(object : Observer<Boolean> {
                override fun onComplete() {
                    startCheck()//递归操作
                }

                override fun onSubscribe(d: Disposable) {
                    disposable = d
                }

                override fun onNext(t: Boolean) {
                    if (t) {
                        mAvailabilityCallback.onChecked(DataAvailability.AVAILABLE)
                    } else {
                        mAvailabilityCallback.onChecked(DataAvailability.UNABAILABLE)
                    }
                }

                override fun onError(e: Throwable) {
                    startCheck()//递归操作
                }
            })
    }

    fun stopCheck() {
//        mHandler.removeCallbacksAndMessages(null)
        disposable?.dispose()//中断下游不再接收
    }

//    class MHandler(private val mNetworkDataAvailabilityInterceptor: NetworkDataAvailabilityInterceptor) : Handler() {
//        override fun handleMessage(msg: Message) {
//            super.handleMessage(msg)
//            val networkOnline = mNetworkDataAvailabilityInterceptor.isNetworkOnline()
//            Log.i("233","networkOnline:$networkOnline")
//        }
//    }

    interface DataAvailabilityCallback {
        fun onChecked(dataAvailability: DataAvailability)
    }

    private fun isNetworkOnline(): Boolean {
        val runtime = Runtime.getRuntime()
        var ipProcess: Process? = null
        try {
            ipProcess = runtime.exec("ping -c 5 -w 4 www.baidu.com")
            val input: InputStream = ipProcess.inputStream
            val `in` = BufferedReader(InputStreamReader(input))
            val stringBuffer = StringBuffer()
            var content: String?
            while (`in`.readLine().also { content = it } != null) {
                stringBuffer.append(content)
            }
            val exitValue = ipProcess.waitFor()
            return if (exitValue == 0) { //WiFi连接，网络正常
                true
            } else {
                stringBuffer.indexOf("100% packet loss") == -1
            }
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: InterruptedException) {
            e.printStackTrace()
        } finally {
            ipProcess?.destroy()
            runtime.gc()
        }
        return false
    }
}