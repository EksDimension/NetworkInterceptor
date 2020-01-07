package com.eks.networkinterceptor

import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import com.eks.networkinterceptor.callback.NetworkInterceptCallback
import com.eks.networkinterceptor.type.NetworkType

/**
 * Created by Riggs on 2019/3/5
 */
class MConnectivityManager(private val mNetworkInterceptCallback: NetworkInterceptCallback) :
    ConnectivityManager.NetworkCallback() {
    /**
     * 当前的连接类型(默认为无连接)
     */
    private var currentStatus: NetworkType = NetworkType.NONE
    /**
     * 原状态(用来记录变化前的状态)
     */
    private var lastStatus: NetworkType = NetworkType.NONE
    /**
     * 存储连接类型的map <连接Id,连接类型>
     */
    private var currentStatusMap: HashMap<Network?, NetworkType> = hashMapOf()

    /**
     * 连接线路更变
     */
    override fun onCapabilitiesChanged(
        network: Network?,
        networkCapabilities: NetworkCapabilities?
    ) {
        super.onCapabilitiesChanged(network, networkCapabilities)
        networkCapabilities?.let {
            if (it.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                //如果连接上了wifi
                currentStatusMap[network] = NetworkType.WIFI
                currentStatus = NetworkType.WIFI
            } else if (it.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                //如果连接上了移动数据
                currentStatusMap[network] = NetworkType.CELLULAR
                currentStatus = NetworkType.CELLULAR
                //遍历下连接map,如果有wifi则以wifi优先
                currentStatusMap.keys.forEach { key ->
                    if (currentStatusMap[key] == NetworkType.WIFI)
                        currentStatus = NetworkType.WIFI
                }
            } else {
                //否则就是其他连接情况
                currentStatusMap[network] = NetworkType.OTHER
                currentStatus = NetworkType.OTHER
                //遍历下连接map,如果有wifi则以wifi优先
                currentStatusMap.keys.forEach { key ->
                    if (currentStatusMap[key] == NetworkType.WIFI)
                        currentStatus = NetworkType.WIFI
                }
            }
            connectStatusChanged()
        }
    }

    /**
     * 连接断开
     */
    override fun onLost(network: Network?) {
        super.onLost(network)
        currentStatusMap.remove(network)
        if (currentStatusMap.isEmpty()) {
            currentStatus = NetworkType.NONE
        } else {
            currentStatusMap.keys.forEach {
                currentStatus = when (currentStatusMap[it]) {
                    NetworkType.WIFI ->
                        NetworkType.WIFI
                    NetworkType.CELLULAR ->
                        NetworkType.CELLULAR
                    else ->
                        NetworkType.OTHER
                }
            }
        }
        connectStatusChanged()
    }

    /**
     * 网络链接发生变化
     */
    private fun connectStatusChanged() {
        if (lastStatus.name != currentStatus.name)
            mNetworkInterceptCallback.onNetworkStatusChanged(currentStatus)
        lastStatus = currentStatus
    }
}