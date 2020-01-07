package com.eks.networkinterceptor.callback

import com.eks.networkinterceptor.type.NetworkType

/**
 * 网络监听回调接口
 * Created by Riggs on 2019/3/5
 */
interface NetworkInterceptCallback {
    /**
     * 网络监听发生变化回调方法
     * @param type 当前变化的网络连接方式
     */
    fun onNetworkStatusChanged(type: NetworkType)
}