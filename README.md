# 一款网络环境的实时监听框架

## 情景需求
> 在咱们app开发过程中，经常都会有判断网络所属环境的需求。比如 **使用移动数据的情况下** 进行 **大文件上传下载、音视频播放** 等情况。都应该给予用户足够的提醒，增加用户体验感。


此时需要的，是一个能够自动监听 **网络连接环境** 的功能。一般我们可以在百度 谷歌上找到一些 **判断当前是否有网** 或者 **判断是否连着wifi** 的代码片段予以封装应用。

------------

> 而另一种情况则是一些对于网络通畅度有要求的项目，例如 **IM通讯项目**、**工业工程项目** 等。当开车进隧道、地铁电梯信号盲区等在网络堵塞时，总需要有一些提示警告、自动重连等操作。


此时需要的，莫过于一个 **数据可用性** 监听功能。这个也许需要自行弄一套类似心跳包检测的功能，网上稍微找找资料也可以应付一下。

------------

而其实，连着Wifi时，也会有更奇怪的情况出现，使用过程中往往都会碰到更为一些  **莫 名 其 妙** 的情况↓↓↓
> ---- 连着Wifi呢，可是Wifi大姨妈，网络堵塞住了。
---- Wifi也连着，也大姨妈了，我这堵住了后，手机却用流量跑起来了，但一直提示我Wifi是连接的。这不坑爹吗？

## 对此
在这里，给大家献上一套能应对上述综合情况的小框架。


### 简单使用指南
#### 版本要求：目前理论上只支持Android 5.0及以上使用。
##### 或者先来个DemoAPK试用下??? [点这下载](https://github.com/EksDimension/NetworkInterceptor/raw/master/app/testDemo.apk "点这下载")

------------



#### 首先配置你的根目录gradle，支持jcenter仓库
```gradle
buildscript {
    ...
    repositories {
        ...
        jcenter()//这个没有就得加上
    }
    ...
}
```

------------

#### 然后针对业务module/library 添加依赖
##### Gradle
```gradle
implementation 'com.eks.framework:network_interceptor:1.0.4'
```

##### Maven
```xml
<dependency>
  <groupId>com.eks.framework</groupId>
  <artifactId>network_interceptor</artifactId>
  <version>1.0.4</version>
  <type>pom</type>
</dependency>
```



------------
#### 详细应用
##### 在Activity中
```java
class TestActivity : AppCompatActivity() , LifecycleOwner {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
	//执行create，并加入lifecycle
        lifecycle.addObserver(NetworkInterceptManager.create(this))
    }

	/**
	 *写上NetworkChange注解，函数名任取，形参有且只有一个，类型为NetworkResponse
	 *这个函数就是接收网络回调用到的
	 *NetworkResponse里面，有2个属性，分别为networkType和availability，均为枚举类
	 *其中networkType枚举类为“有效连接的网络类型”，值分别有：有WIFI、CELLULAR（移动数据）、OTHER（其他连接，如蓝牙/VPN之类的，但还没具体测出）、NONE（无任何连接）及WAITING（等待检测）。该属性的切换速度较快，只要连接得以搭建或者失去连接状态后就会立即有效。
	 *而availability枚举类为“数据可用性”，值分别有：AVAILABLE（可用,畅通）、UNABAILABLE（不可用,阻塞）、WAITING（等待检测）。该属性切换速度相对会有延迟，因为框架会针对网络进行连通性检测，需要一定的“容错重试”时间。
	 */
    @NetworkChange
    fun onNetworkChanged(res: NetworkResponse) {

	}
}
```
##### 在Fragment中
```java
class TestFragment : Fragment() , LifecycleOwner {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
	//执行create，并加入lifecycle
        lifecycle.addObserver(NetworkInterceptManager.create(this))
        return super.onCreateView(inflater, container, savedInstanceState)
    }

	//跟acivity一样 不重复写了
    @NetworkChange
    fun onNetworkChanged(res: NetworkResponse) {
    }
}
```

至于回调函数里面那个形参, 注释里面这样写可能会有点晕, 那就做个小表格意思意思下

举个例子吧

|情形\参数|res.networkType.name|res.availability.name|
| ------------ | ------------ | ------------ |
|wifi连接且畅通时|WIFI|AVAILABLE|
|wifi连接, 而且真的上不了网(wifi大姨妈,光猫失灵,车载wifi进入了信号盲区)|WIFI|UNABAILABLE|
|wifi连接, 却用移动数据跑通了网|CELLULAR|AVAILABLE|
|在用移动数据畅通上网|CELLULAR|AVAILABLE|
|在用移动数据但网络堵塞|CELLULAR|UNABAILABLE|
|还有更多还没想到,待验证|to be continued...|to be continued...|

##### 当然，如果你实在想手动获取当前网络状况，也不是不行，可以通过执行NetworkInterceptManager.currentType.name和NetworkInterceptManager.currentAvailability.name 就可以直接获取了，或者像下面代码那样toast一波也行
```java
btnTest.setOnClickListener {
            Toast.makeText(
                this@FirstActivity,
                "当前网络状况:\n类型${NetworkInterceptManager.currentType.name}\n可用性${NetworkInterceptManager.currentAvailability.name}", Toast.LENGTH_SHORT
            ).show()
        }
```


------------



### 既然可以测试网络数据有效性，那么我可以仅指定连接的服务器吗？
##### 可以！！！
比如整个App里面，一共用到3个已知的服务器ip，分别是119.124.21.213 120.124.129.23 115.129.59.23 端口分别为80 81 82
那么在Application里头设置自定义服务器即可
```java
class TestApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        NetworkInterceptManager.setCustomServers(
            arrayOf(
                SocketAddressForTesting("119.124.21.213", 80),
                SocketAddressForTesting("120.124.129.23", 81),
                SocketAddressForTesting("115.129.59.23", 82)
            )
        )
    }
}
```
如果你不是想针对整个APP，而仅仅在指定时刻指定服务器，其余情况恢复默认服务器。
那你就在你需要指定的地方运行上述的代码。想在恢复默认服务器，则运行下方代码即可。
```java
NetworkInterceptManager.setCustomServers(null)
```

------------
## 温馨提示
由于这还是首次发布个人的通用框架，里面还有很多细节等待完善，距离全面商业级应用还有很大段路要走，处于局部测试阶段中，有任何不足之处、技术建议，还请大家多多指点 多多批评。

#### 本框架部分核心技术、思想、灵感来源于 彭锡老师-Simon[（点击访问博客）](https://www.cmonbaby.com/ "（点这里访问）")，轮循器封装由zhongruiAndroid提供[（点击访问Git主页）](https://github.com/zhongruiAndroid "（点击访问Git主页）")，特此表示感谢。也感谢所有加入测试行列 提出任何意见的小伙伴们。
