package com.eks.networkinterceptor.bean

import com.eks.networkinterceptor.type.Availability
import com.eks.networkinterceptor.type.NetworkType

/**
 * Created by Riggs on 1/10/2020
 */
data class NetworkResponse(val networkType: NetworkType, val available: Availability)