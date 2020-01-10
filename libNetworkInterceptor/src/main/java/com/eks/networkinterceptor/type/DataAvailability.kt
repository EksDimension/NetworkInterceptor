package com.eks.networkinterceptor.type

/**
 * 网络数据连通有效性枚举类
 * Created by Riggs on 1/10/2020
 */
enum class DataAvailability {
    /**
     * 可用,畅通
     */
    AVAILABLE,
    /**
     * 不可用,阻塞
     */
    UNABAILABLE,
    /**
     * 等待检测
     */
    WAITING
}