package com.eks.networkinterceptor

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
import com.eks.networkinterceptor.annotation.NetworkChange
import com.eks.networkinterceptor.bean.MethodBean
import com.eks.networkinterceptor.callback.NetworkInterceptCallback
import com.eks.networkinterceptor.type.NetworkType
import io.reactivex.Observable
import io.reactivex.Observer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers

/**
 * Created by Riggs on 2019/3/4
 */
object NetworkInterceptManager {

    private lateinit var mApplication: Application
    private lateinit var cmgr: ConnectivityManager
    private var mMethodsMap: HashMap<Any, List<MethodBean>> = hashMapOf()
    private var mNIHandler = NIHandler()

    private var HANDLER_MSG_SWITCH_TO_MAIN_THREAD = 1

    fun init(application: Application) {
        mApplication = application
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {// 5.0及之后可以通过ConnectivityManager进行网络的监听
            val builder = NetworkRequest.Builder()
            val request = builder.build()
            cmgr =
                mApplication.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            cmgr.registerNetworkCallback(request, MConnectivityManager(mNetworkInterceptCallback))
            // if (cmgr != null) cmgr.unregisterNetworkCallback(networkCallback);
        } else {//5.0之前最好就是使用广播,而且动态广播为妙
        }
    }

    fun bind(obj: Any) {
        val foundMethods = findAnnotationMethods(obj)
        mMethodsMap[obj] = foundMethods
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
     * 获取当前网络状况
     */
    @TargetApi(Build.VERSION_CODES.M)
    fun getCurrentNetworkStatus(mCallback: INetworkStatusCallback) {
        Observable.create<NetworkType> { e ->
            val type: NetworkType
            if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)) {//6.0后用activeNetwork获取
                val networkCapabilities = cmgr.getNetworkCapabilities(cmgr.activeNetwork)
                if (networkCapabilities == null) {
                    type = NetworkType.NONE
                } else {
                    when {
                        networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> {
                            type = NetworkType.WIFI
                        }
                        networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> {
                            type = NetworkType.CELLULAR
                        }
                        else -> {
                            type = NetworkType.NONE
                        }
                    }
                }
            } else {//低于6.0用activeNetworkInfo
                val activeNetworkInfo = cmgr.activeNetworkInfo
                if (activeNetworkInfo == null || !activeNetworkInfo.isAvailable || !activeNetworkInfo.isConnected) {
                    type = NetworkType.NONE
                } else {
                    when (activeNetworkInfo.type) {
                        -1 -> type = NetworkType.NONE
                        ConnectivityManager.TYPE_MOBILE -> type = NetworkType.CELLULAR
                        ConnectivityManager.TYPE_WIFI -> type = NetworkType.WIFI
                        else -> type = NetworkType.OTHER
                    }
                }
            }
            e.onNext(type)
        }.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : Observer<NetworkType>{
                override fun onComplete() {
                }

                override fun onSubscribe(d: Disposable) {
                }

                override fun onNext(t: NetworkType) {
                    mCallback.currentStatus(t)
                }

                override fun onError(e: Throwable) {
                }
            })
//            .subscribe {
//                mCallback.currentStatus(it)
//            }
    }

    interface INetworkStatusCallback {
        fun currentStatus(networkType: NetworkType)
    }

}