package com.eks.netlistener

import android.os.Bundle
import android.util.Log
import com.eks.networkinterceptor.annotation.NetworkChange
import com.eks.networkinterceptor.type.NetworkType

class MainActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    @NetworkChange
    fun onNetworkChanged(type: NetworkType) {
        Log.i("23333", type.name)
    }


}
