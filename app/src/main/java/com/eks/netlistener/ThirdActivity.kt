package com.eks.netlistener

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import com.eks.networkinterceptor.annotation.NetworkChange
import com.eks.networkinterceptor.type.NetworkType
import kotlinx.android.synthetic.main.activity_first.*

/**
 * Created by Riggs on 1/7/2020
 */

@SuppressLint("SetTextI18n")
class ThirdActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_first)
        btnNavigate.visibility = View.GONE
    }

    @NetworkChange
    fun onNetworkChanged(type: NetworkType) {
        textView1.text = "第三页实时网络状况:${type.name}"
    }

}
