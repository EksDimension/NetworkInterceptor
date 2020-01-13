package com.eks.networkinterceptor

import android.os.SystemClock
import android.util.Log
import com.eks.networkinterceptor.bean.SocketAddressForTesting
import com.eks.networkinterceptor.type.DataAvailability
import io.reactivex.Observable
import io.reactivex.Observer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import java.net.InetSocketAddress
import java.net.Socket


/**
 * 检测网络是否真正连通
 * Created by Riggs on 1/10/2020
 */
class NetworkDataAvailabilityInterceptor(
    private val mAvailabilityCallback: DataAvailabilityCallback,
    private var customeServers: Array<SocketAddressForTesting>? = null
) {


    private var disposable: Disposable? = null


    fun startCheck() {
        disposable?.dispose()//再中断一次之前的下游 确保同时只执行一次任务
        Observable.create<Boolean> {
            SystemClock.sleep(5000)
            val networkOnline = isDataAvailable()
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

    /**
     * 检查数据是否可用
     */
    private fun isDataAvailable(): Boolean {
        connectSucceed = false
        val servers: Array<SocketAddressForTesting>? =
            if (customeServers != null && customeServers?.isNotEmpty() == true) {
                //如果有使用自定义服务器
                //那就用自定义服务器去测试
                customeServers

            } else {
                defaultServers
            }
        //对多个服务器进行遍历连接
        servers?.forEach {
            val isConnected = checkBySocket(it)
            Log.i("233", "服务器:${it.hostName} 端口:${it.port} 是否成功:$isConnected")
            if (isConnected) connectSucceed = true
        }
        return connectSucceed
    }

    fun stopCheck() {
        disposable?.dispose()//中断下游不再接收
    }

    interface DataAvailabilityCallback {
        fun onChecked(dataAvailability: DataAvailability)
    }

    private var socket: Socket? = null

    private fun checkBySocket(server: SocketAddressForTesting): Boolean {

        try {
            if (socket != null) {
                socket?.close()
                socket = null
            }
            socket = Socket()
            val inetSocketAddress = InetSocketAddress(server.hostName, server.port)
            socket?.connect(inetSocketAddress, 4000)
        } catch (e: Exception) {

        } finally {
            socket?.close()
            val isConnected = socket?.isConnected ?: false
            socket = null
            return isConnected
        }
    }

    /**
     * 服务器是否连通(有任意一个连通了都行)
     */
    private var connectSucceed = false
    /**
     * 默认策略
     */
    private var defaultServers = arrayOf(
        SocketAddressForTesting("www.baidu.com", 80),
        SocketAddressForTesting("www.163.com", 80),
        SocketAddressForTesting("www.qq.com", 80),
        SocketAddressForTesting("www.1688.com", 80)
    )
}