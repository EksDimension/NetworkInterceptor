package com.eks.netlistener

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.eks.networkinterceptor.NetworkInterceptManager

/**
 * Created by Riggs on 2019/3/5
 */
abstract class BaseActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        NetworkInterceptManager.bind(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        NetworkInterceptManager.unbind(this)
    }
}