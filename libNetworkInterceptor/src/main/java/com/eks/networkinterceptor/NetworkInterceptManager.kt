package com.eks.networkinterceptor

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.os.Handler
import android.os.Handler.Callback
import android.os.Message
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import com.eks.networkinterceptor.annotation.NetworkChange
import com.eks.networkinterceptor.bean.MethodBean
import com.eks.networkinterceptor.bean.NetworkResponse
import com.eks.networkinterceptor.bean.SocketAddressForTesting
import com.eks.networkinterceptor.callback.NetworkInterceptCallback
import com.eks.networkinterceptor.thread.ThreadPoolManager
import com.eks.networkinterceptor.type.DataAvailability
import com.eks.networkinterceptor.type.NetworkType
import com.eks.networkinterceptor.type.WifiAvailability

/**
 * 网络拦截管理器
 * Created by Riggs on 2019/3/4
 */
object NetworkInterceptManager : LifecycleObserver {

    private var TAG = NetworkInterceptManager.javaClass.simpleName

    private var cmgr: ConnectivityManager? = null
    private var mMethodsMap: HashMap<Any, List<MethodBean>> = hashMapOf()
    @JvmField
    var currentType: NetworkType = NetworkType.WAITING
    @JvmField
    var currentAvailability: DataAvailability = DataAvailability.WAITING
    private var mCustomeServers: Array<SocketAddressForTesting>? = null

    private var HANDLER_MSG_SWITCH_TO_MAIN_THREAD = 1

//    fun init(): NetworkInterceptManager {
//        return this
//    }

    /**
     * 注册拦截器,只有第一个对象被绑定才注册
     */
    private fun installInterceptor() {
        if (mMethodsMap.isNotEmpty()) return
        val builder = NetworkRequest.Builder()
        val request = builder.build()
        cmgr =
            AppGlobals.getApplication()?.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        cmgr?.registerNetworkCallback(request, mNetworkCallback)
        mNetworkDataAvailabilityInterceptor.startCheck()
        mWifiAvailabilityInterceptor.startCheck()
    }

    /**
     * 反注册拦截器,只有最后一个对象被解绑后才反注册
     */
    private fun uninstallInterceptor() {
        if (mMethodsMap.isNotEmpty()) return
        cmgr?.unregisterNetworkCallback(mNetworkCallback)
    }

    /**
     * 创建-绑定对象 对应onCreate/onCreateView
     * @param obj activity或fragment对象
     */
    @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
    fun create(obj: Any): NetworkInterceptManager {
        installInterceptor()
        val foundMethods = findAnnotationMethods(obj)
        mMethodsMap[obj] = foundMethods
        checkImmed()
        return this
    }

    /**
     * 恢复-对应onResume
     */
    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    private fun resume() {
        mNetworkDataAvailabilityInterceptor.startCheck()
        mWifiAvailabilityInterceptor.startCheck()
    }

    /**
     * 暂停-对应onPause
     */
    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    private fun pause() {
        mNetworkDataAvailabilityInterceptor.stopCheck()
        mWifiAvailabilityInterceptor.stopCheck()
    }

    /**
     * 销毁-解绑对象 对应onDestroyed/onDestroyedView
     * @param obj activity或fragment对象
     */
    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    private fun destroy(obj: Any) {
        mMethodsMap.remove(obj)
        uninstallInterceptor()
    }

    /**
     * 解绑所有对象
     */
    fun unbindAll() {
        mMethodsMap.clear()
        uninstallInterceptor()
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
                if (!method.parameterTypes[0].isAssignableFrom(NetworkResponse::class.java)) return@forEach
                methodBeanLists.add(MethodBean(method))
            }
        }
        return methodBeanLists
    }


    private var mNetworkInterceptCallback: NetworkInterceptCallback =
        object : NetworkInterceptCallback {
            override fun onNetworkStatusChanged(type: NetworkType) {
                currentType = type
                //由于在这里操作的线程是属于ConnectivityThread而非主线程,因此需要统一切换下
                val message = Message.obtain()
                message.what = HANDLER_MSG_SWITCH_TO_MAIN_THREAD
                mNIHandler.sendMessage(message)
            }
        }

    private val mNetworkCallback = MNetworkCallback(mNetworkInterceptCallback)

    private var mNIHandler = NIHandler()

    private class NIHandler : Handler(Callback {
        if (it.what == HANDLER_MSG_SWITCH_TO_MAIN_THREAD) {
            //遍历所有已绑定的类及对应的函数集合
            mMethodsMap.keys.forEach { obj ->
                val methodList = mMethodsMap[obj]
                //遍历函数集合
                methodList?.forEach { methodBean ->
//                    Log.i(TAG, "网络连接发生变化：${currentType.name} ${currentAvailability.name}")
                    //反射执行函数
                    methodBean.method.invoke(obj, NetworkResponse(currentType, currentAvailability))
                }
            }
        }
        true
    })

    fun setCustomServers(customeServers: Array<SocketAddressForTesting>?) {
        mCustomeServers = customeServers
        mNetworkDataAvailabilityInterceptor.setCustomServers(mCustomeServers)
    }

    private val mNetworkDataAvailabilityInterceptor =
        NetworkDataAvailabilityInterceptor(object :
            NetworkDataAvailabilityInterceptor.DataAvailabilityCallback {
            override fun onChecked(dataAvailability: DataAvailability) {
                if (currentAvailability != dataAvailability) {
//                    Log.i(TAG, "网络数据可用性变化:${dataAvailability.name}")
                    currentAvailability = dataAvailability
                    mNetworkInterceptCallback.onNetworkStatusChanged(currentType)
                }
            }
        })

    private val mWifiAvailabilityInterceptor =
        WifiAvailabilityInterceptor(object : WifiAvailabilityInterceptor.WifiAvailabilityCallback {
            override fun onChecked(
                wifiAvailability: WifiAvailability,
                wifiNetwork: Network?
            ) {
                if (wifiNetwork != null && wifiAvailability == WifiAvailability.CONNECTED_BUT_UNAVAILABLE) {
//                    Log.i(TAG, "Wifi已连接但不可用")
                    //有连wifi 但wifi不可用. 那就得要去掉wifi网络
                    mNetworkCallback.onLost(wifiNetwork)
                } else {
//                    Log.i(TAG, "Wifi未连接 或完全可用")
                }
            }
        })

    /**
     * 立即获取网络状态
     */
    @TargetApi(Build.VERSION_CODES.M)
    @Suppress("DEPRECATION")
    @SuppressLint("CheckResult")
    private fun checkImmed() {
//        Observable.create<NetworkType> { e ->
//            val type: NetworkType
//            if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)) {//6.0后用activeNetwork获取
//                val networkCapabilities = cmgr?.getNetworkCapabilities(cmgr?.activeNetwork)
//                type = if (networkCapabilities == null) {
//                    NetworkType.NONE
//                } else {
//                    when {
//                        networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> {
//                            NetworkType.WIFI
//                        }
//                        networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> {
//                            NetworkType.CELLULAR
//                        }
//                        else -> {
//                            NetworkType.NONE
//                        }
//                    }
//                }
//            } else {//低于6.0用activeNetworkInfo
//                val activeNetworkInfo = cmgr?.activeNetworkInfo
//                type =
//                    if (activeNetworkInfo == null || !activeNetworkInfo.isAvailable || !activeNetworkInfo.isConnected) {
//                        NetworkType.NONE
//                    } else {
//                        when (activeNetworkInfo.type) {
//                            -1 -> NetworkType.NONE
//                            ConnectivityManager.TYPE_MOBILE -> NetworkType.CELLULAR
//                            ConnectivityManager.TYPE_WIFI -> NetworkType.WIFI
//                            else -> NetworkType.OTHER
//                        }
//                    }
//            }
//            e.onNext(type)
//        }.subscribeOn(Schedulers.io())
//            .observeOn(AndroidSchedulers.mainThread())
//            .subscribe { mNetworkInterceptCallback.onNetworkStatusChanged(it) }
        ThreadPoolManager.getInstance().execute {
            val type: NetworkType
            if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)) {//6.0后用activeNetwork获取
                val networkCapabilities = cmgr?.getNetworkCapabilities(cmgr?.activeNetwork)
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
                val activeNetworkInfo = cmgr?.activeNetworkInfo
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
            ThreadPoolManager.getInstance().runOnUiThread {
                mNetworkInterceptCallback.onNetworkStatusChanged(type)
            }
        }
    }
}