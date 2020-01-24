package com.eks.netlistener

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LifecycleOwner
import com.eks.networkinterceptor.NetworkInterceptManager

/**
 * Created by Riggs on 2019/3/5
 */
abstract class BaseActivity : AppCompatActivity(), LifecycleOwner {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycle.addObserver(NetworkInterceptManager.create(this))
    }

//    override fun onResume() {
//        super.onResume()
//        NetworkInterceptManager.resume()
//    }
//
//    override fun onPause() {
//        super.onPause()
//        NetworkInterceptManager.pause()
//    }
//
//    override fun onDestroy() {
//        super.onDestroy()
//        NetworkInterceptManager.destroy(this)
//    }
}