package com.eks.netlistener

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import com.eks.networkinterceptor.NetworkInterceptManager
import com.eks.networkinterceptor.annotation.NetworkChange
import com.eks.networkinterceptor.bean.NetworkResponse
import kotlinx.android.synthetic.main.activity_first.*

/**
 * Created by Riggs on 1/7/2020
 */

@SuppressLint("SetTextI18n")
class SecondActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_first)
        btnNavigate.setOnClickListener {
            startActivity(Intent(this@SecondActivity, ThirdActivity::class.java))
        }
        btn1.setOnClickListener {
            Toast.makeText(
                this@SecondActivity,
                "当前网络状况:${NetworkInterceptManager.currentType.name}", Toast.LENGTH_SHORT
            ).show()
        }
    }

    @NetworkChange
    fun onNetworkChanged(res: NetworkResponse) {
        textView1.text = "第二页实时网络状况:${res.networkType.name}"
    }

}
