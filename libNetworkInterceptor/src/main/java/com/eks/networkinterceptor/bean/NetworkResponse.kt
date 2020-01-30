package com.eks.networkinterceptor.bean

import com.eks.networkinterceptor.type.DataAvailability
import com.eks.networkinterceptor.type.NetworkType

/**
 * 统一返回实体类
 * Created by Riggs on 1/10/2020
 */
data class NetworkResponse(@JvmField val networkType: NetworkType,@JvmField val availability: DataAvailability)