package com.eks.networkinterceptor

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.os.Handler
import android.os.Handler.Callback
import android.os.Message
import android.util.Log
import com.eks.networkinterceptor.annotation.NetworkChange
import com.eks.networkinterceptor.bean.MethodBean
import com.eks.networkinterceptor.callback.NetworkInterceptCallback
import com.eks.networkinterceptor.type.NetworkType
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers

/**
 * Created by Riggs on 2019/3/4
 */
object NetworkInterceptManager {

    private lateinit var mApplication: Application
    private lateinit var cmgr: ConnectivityManager
    private var mMethodsMap: HashMap<Any, List<MethodBean>> = hashMapOf()
    private var mNIHandler = NIHandler()
    var currentStatus: NetworkType = NetworkType.NONE

    private var HANDLER_MSG_SWITCH_TO_MAIN_THREAD = 1

    fun init(application: Application) {
        mApplication = application
        val builder = NetworkRequest.Builder()
        val request = builder.build()
        cmgr =
            mApplication.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        cmgr.registerNetworkCallback(request, MNetworkCallback(mNetworkInterceptCallback))
        // if (cmgr != null) cmgr.unregisterNetworkCallback(networkCallback);
        checkNetDataAvailability()
    }

    @SuppressLint("SoonBlockedPrivateApi")
    private fun checkNetDataAvailability() {
        //获取mCallback属性
        val mCallbackFiled = Handler::class.java.getDeclaredField("mCallback")
        mCallbackFiled.isAccessible = true // 授权

        //获取ConnectivityManager类class
        val mConnectivityManagerClass = Class.forName("android.net.ConnectivityManager")
        //获取ConnectivityManager下面的静态属性字段sCallbackHandler
        val msCallbackHandlerField = mConnectivityManagerClass.getDeclaredField("sCallbackHandler")
        msCallbackHandlerField.isAccessible = true
        //把sCallbackHandler属性实例化 类型为ConnectivityManager.CallbackHandler
        val mCallbackHandler = msCallbackHandlerField.get(cmgr) as Handler
        mCallbackFiled.set(mCallbackHandler, MyCallback(mCallbackHandler))//替换Handler里头的Callback回调
    }

    class MyCallback(private val mCallbackHandler: Handler) : Callback {
        override fun handleMessage(msg: Message): Boolean {
            Log.i("233", msg.what.toString())//<<<---这里没有收到消息
            mCallbackHandler.handleMessage(msg)
            return true
        }
    }


    fun bind(obj: Any) {
        val foundMethods = findAnnotationMethods(obj)
        mMethodsMap[obj] = foundMethods
        checkImmed()
    }

    fun unbind(obj: Any) {
        mMethodsMap.remove(obj)
    }

    fun unbindAll() {
        mMethodsMap.clear()
    }

    /**
     * 从类里面收集方法
     * @param obj 类实例对象
     */
    private fun findAnnotationMethods(obj: Any): ArrayList<MethodBean> {
        val methodBeanLists: ArrayList<MethodBean> = arrayListOf()
        //找出类所有的方法 包括父类的
        val methods = obj.javaClass.methods
        //遍历
        methods.forEach { method ->
            //去除java android androidx开头的源码文件
            if (method.declaringClass.toString().startsWith("class androidx.")
                || method.declaringClass.toString().startsWith("class android.")
                || method.declaringClass.toString().startsWith("class java.")
            ) return@forEach
            //过滤保留指定注解的函数
            val annotation = method.getAnnotation(NetworkChange::class.java)
            annotation?.let {
                //如果函数返回值不是void,不要
                val returnType = method.returnType
                if (returnType.toString() != "void") return@forEach
                //如果函数参数长度不为1,而且不是指定枚举类型,也不要
                if (method.parameterTypes.size != 1) return@forEach
                if (!method.parameterTypes[0].isAssignableFrom(NetworkType::class.java)) return@forEach
                methodBeanLists.add(MethodBean(method))
            }
        }
        return methodBeanLists
    }

    private var mNetworkInterceptCallback: NetworkInterceptCallback =
        object : NetworkInterceptCallback {
            override fun onNetworkStatusChanged(type: NetworkType) {
                currentStatus = type
                //由于在这里操作的线程是属于ConnectivityThread而非主线程,因此需要统一切换下
                val message = Message.obtain()
                message.what = HANDLER_MSG_SWITCH_TO_MAIN_THREAD
                message.obj = type
                mNIHandler.sendMessage(message)
            }
        }

    internal class NIHandler : Handler(Callback {
        if (it.what == HANDLER_MSG_SWITCH_TO_MAIN_THREAD) {
            //遍历所有已绑定的类及对应的函数集合
            mMethodsMap.keys.forEach { obj ->
                val methodList = mMethodsMap[obj]
                //遍历函数集合
                methodList?.forEach { methodBean ->
                    //反射执行函数
                    methodBean.method.invoke(obj, it.obj)
                }
            }
        }
        true
    })


    /**
     * 立即获取网络状态
     */
    @Suppress("DEPRECATION")
    @SuppressLint("CheckResult")
    @TargetApi(Build.VERSION_CODES.M)
    private fun checkImmed() {
        Observable.create<NetworkType> { e ->
            val type: NetworkType
            if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)) {//6.0后用activeNetwork获取
                val networkCapabilities = cmgr.getNetworkCapabilities(cmgr.activeNetwork)
                type = if (networkCapabilities == null) {
                    NetworkType.NONE
                } else {
                    when {
                        networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> {
                            NetworkType.WIFI
                        }
                        networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> {
                            NetworkType.CELLULAR
                        }
                        else -> {
                            NetworkType.NONE
                        }
                    }
                }
            } else {//低于6.0用activeNetworkInfo
                val activeNetworkInfo = cmgr.activeNetworkInfo
                type =
                    if (activeNetworkInfo == null || !activeNetworkInfo.isAvailable || !activeNetworkInfo.isConnected) {
                        NetworkType.NONE
                    } else {
                        when (activeNetworkInfo.type) {
                            -1 -> NetworkType.NONE
                            ConnectivityManager.TYPE_MOBILE -> NetworkType.CELLULAR
                            ConnectivityManager.TYPE_WIFI -> NetworkType.WIFI
                            else -> NetworkType.OTHER
                        }
                    }
            }
            e.onNext(type)
        }.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { mNetworkInterceptCallback.onNetworkStatusChanged(it) }
    }
}