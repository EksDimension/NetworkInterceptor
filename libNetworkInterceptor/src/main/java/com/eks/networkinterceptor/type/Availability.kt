package com.eks.networkinterceptor.type

/**
 * Created by Riggs on 1/10/2020
 */
enum class Availability {
    /**
     * 已连通(畅通)
     */
    AVAILABLE,
    /**
     * 未连通(阻塞)
     */
    UNABAILABLE,
    /**
     * 等待检测
     */
    WAITING
}