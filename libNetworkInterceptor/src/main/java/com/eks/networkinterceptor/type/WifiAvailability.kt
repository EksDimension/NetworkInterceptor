package com.eks.networkinterceptor.type

/**
 * wifi连通有效性枚举类
 * Created by Riggs on 1/13/2020
 */
enum class WifiAvailability {
    /**
     * WIFI根本没连接或者完全可用
     */
    UNCONNECTED_OR_AVAILABLE,
    /**
     * WIFI已连上但不可用
     */
    CONNECTED_BUT_UNAVAILABLE,
    /**
     * 等待检测
     */
    WAITING
}