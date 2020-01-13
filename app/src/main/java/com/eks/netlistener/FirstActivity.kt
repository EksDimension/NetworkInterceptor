package com.eks.netlistener

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import com.eks.networkinterceptor.NetworkInterceptManager
import com.eks.networkinterceptor.annotation.NetworkChange
import com.eks.networkinterceptor.bean.NetworkResponse
import kotlinx.android.synthetic.main.activity_first.*

@SuppressLint("SetTextI18n")
class FirstActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_first)
        btnNavigate.setOnClickListener {
            startActivity(Intent(this@FirstActivity, SecondActivity::class.java))
        }
        btn1.setOnClickListener {
            Toast.makeText(
                this@FirstActivity,
                "当前网络状况:\n类型${NetworkInterceptManager.currentType.name} 数据${NetworkInterceptManager.currentAvailability.name}", Toast.LENGTH_SHORT
            ).show()
        }
    }

    @NetworkChange
    fun onNetworkChanged(res: NetworkResponse) {
        textView1.text = "第一页实时网络状况:\n类型${res.networkType.name} 数据${res.availability.name}"
    }
}
