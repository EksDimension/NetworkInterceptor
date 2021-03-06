package com.eks.networkinterceptor.type

/**
 * 连接枚举类型
 * Created by Riggs on 2019/3/5
 */
enum class NetworkType {
    /**
     * WIFI
     */
    WIFI,
    /**
     * 移动数据
     */
    CELLULAR,
    /**
     * 蓝牙/VPN等等的其他类型
     */
    OTHER,
    /**
     * 无连接
     */
    NONE,
    /**
     * 等待检测
     */
    WAITING,
}