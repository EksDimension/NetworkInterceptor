package com.eks.netlistener

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import com.eks.networkinterceptor.NetworkInterceptManager
import com.eks.networkinterceptor.annotation.NetworkChange
import com.eks.networkinterceptor.type.NetworkType
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
                "当前网络状况:${NetworkInterceptManager.currentStatus.name}", Toast.LENGTH_SHORT
            ).show()
        }
    }

    @NetworkChange
    fun onNetworkChanged(type: NetworkType) {
        textView1.text = "第一页实时网络状况:${type.name}"
    }
}
