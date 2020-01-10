package com.eks.netlistener

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.widget.Toast
import com.eks.networkinterceptor.NetworkInterceptManager
import com.eks.networkinterceptor.annotation.NetworkChange
import com.eks.networkinterceptor.bean.NetworkResponse
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
        btn1.setOnClickListener {
            Toast.makeText(
                this@ThirdActivity,
                "当前网络状况:${NetworkInterceptManager.currentType.name}", Toast.LENGTH_SHORT
            ).show()
        }
    }

    @NetworkChange
    fun onNetworkChanged(res: NetworkResponse) {
        textView1.text = "第三页实时网络状况:${res.networkType.name}"
    }

}
