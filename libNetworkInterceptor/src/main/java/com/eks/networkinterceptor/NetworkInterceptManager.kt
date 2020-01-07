package com.eks.networkinterceptor

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkRequest
import android.os.Build
import com.eks.networkinterceptor.annotation.NetworkChange
import com.eks.networkinterceptor.bean.MethodBean
import com.eks.networkinterceptor.callback.NetworkInterceptCallback
import com.eks.networkinterceptor.type.NetworkType

/**
 * Created by Riggs on 2019/3/4
 */
object NetworkInterceptManager {

    private var mApplication: Application? = null
    private var mMethodsMap: HashMap<Any, List<MethodBean>> = hashMapOf()

    fun init(application: Application) {
        mApplication = application
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {// 5.0及之后可以通过ConnectivityManager进行网络的监听
            val builder = NetworkRequest.Builder()
            val request = builder.build()
            val cmgr = mApplication?.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            cmgr?.registerNetworkCallback(request, MConnectivityManager(mNetworkInterceptCallback))
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

    fun unbindAll(){
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

    private var mNetworkInterceptCallback: NetworkInterceptCallback = object : NetworkInterceptCallback {
        override fun onNetworkStatusChanged(type: NetworkType) {
            //遍历所有已绑定的类及对应的函数集合
            mMethodsMap.keys.forEach { obj ->
                val methodList = mMethodsMap[obj]
                //遍历函数集合
                methodList?.forEach { methodBean ->
                    //反射执行函数
                    methodBean.method.invoke(obj, type)
                }
            }
        }
    }

}