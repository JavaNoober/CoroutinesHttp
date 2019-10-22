# CoroutinesHttp
基于retrofit以及协程的dsl形式的网络请求

## 前言
协程正式版出来已经有一段时间，相对于线程，协程占用更小的资源同时也可以更加方便的进行各个线程的切换。从retrofit2.6.0开始，retrofit直接可以支持哦诶和协程的使用。那么接下来就给大家展示一下如何快速封装一个基于协程的dsl形式的请求方法。  
文章内容以目前较为常用的mvp架构为例。
## 封装后的请求方式

    /**
     * 打印结果如下：
     * 
     * MainPresenter: start doHttpRequest:currentThreadName:main
     * MainPresenter: request doHttpRequest:currentThreadName:DefaultDispatcher-worker-2
     * MainPresenter: onSuccess doHttpRequest:currentThreadName:main
     * MainPresenter: UserBean(login=null, id=61097549, node_id=MDEwOlJlcG9zaXRvcnk2MTA5NzU0OQ==, avatar_url=null, gravatar_id=null, url=https://api.github.com/repos/JavaNoober/Album, html_url=https://github.com/JavaNoober/Album, followers_url=null, following_url=null, gists_url=null, starred_url=null, subscriptions_url=null, organizations_url=null, repos_url=null, events_url=https://api.github.com/repos/JavaNoober/Album/events, received_events_url=null, type=null, site_admin=false, name=Album, company=null, blog=null, location=null, email=null, hireable=null, bio=null, public_repos=0, public_gists=0, followers=0, following=0, created_at=2016-06-14T06:28:05Z, updated_at=2016-06-14T06:40:26Z)
     * MainPresenter: onComplete doHttpRequest:currentThreadName:main
     */
    fun doHttpRequest() {
        start {
            Log.e(TAG, "start doHttpRequest:currentThreadName:${Thread.currentThread().name}")
        }.request {
            Log.e(TAG, "request doHttpRequest:currentThreadName:${Thread.currentThread().name}")
            RetrofitHelper.getApi().getUserInfo()
        }.then(onSuccess = {
            Log.e(TAG, "onSuccess doHttpRequest:currentThreadName:${Thread.currentThread().name}")
            Log.e(TAG, it[0].toString())
        }, onError = {
            Log.e(TAG, "onError doHttpRequest:currentThreadName:${Thread.currentThread().name}")
        }) {
            Log.e(TAG, "onComplete doHttpRequest:currentThreadName:${Thread.currentThread().name}")
        }
    }
    
从这个最基本的请求可以看出：
- start为主线程操作，我们可以进行一些ui操作比如弹窗等；当然如果不需要直接进行第二步request操作也可以
- request为网络请求操作，该线程为子线程
- then为网络请求结果，有onError和onSucces以及onComplete方法，为主线程操作
- 最后一个大括号内为onCompete方法，有没有都行，这里是kotlin的lambda形式的写法  

相对于rxjava的方式，这种方式更加的简单，最简单的时候一个request以及then两个操作符就可以进行一次网络请求。同时这种方式是可以防止内存泄露的，可以在activity已经finish的时候自动取消请求，而rxjava如果需要进行防止内存泄漏，需要较为复杂的处理。

## retrofit
### RetrofitHelper
retrofit不再多说，这里创建一个RetrofitHelper类用于进行网络请求。这里为了方便就直接创建类一个retorfit的对象，并没有进行缓存什么的。开发者可以根据自己的需要去进行一些封装。主要区别是，基于协程的retrofit网络请求，不需要像rxjava一样在创建retrofit的时候add一个adapter，直接创建即可。

    class RetrofitHelper {
    
    
        companion object{
            fun getApi(): ApiService{
                val client = OkHttpClient.Builder()
                    .connectTimeout(10, TimeUnit.SECONDS)       //连接超时
                    .readTimeout(10, TimeUnit.SECONDS)          //读取超时
                    .writeTimeout(10, TimeUnit.SECONDS)         //写超时
                    .build()
                val retrofit = Retrofit.Builder().client(client).baseUrl("https://api.github.com/")
                    .addConverterFactory(GsonConverterFactory.create()).build()
    
                return retrofit.create(ApiService::class.java)
            }
        }
    }
    
### ApiService
主要注意的一点是，网络请求的接口方法需要用**suspend**修饰，因为改方法需要在协程内进行，否则请求的时候会报错。

    interface ApiService {
    
        @GET("users/JavaNoober/repos")
        suspend fun getUserInfo(): List<UserBean>
    }
    
## CoroutineDSL

因为需要对请求进行页面结束的时候自动取消处理，这里使用google提供的lifecycle库进行封装，因为在高版本的support库中，lifecycle已经默认集成在里面，这样使用更加的方便，几行代码就你完成所需功能。
### start

    /**
     * execute in main thread
     * @param start doSomeThing first
     */
    infix fun LifecycleOwner.start(start: (() -> Unit)): LifecycleOwner{
        GlobalScope.launch(Main) {
            start()
        }
        return this
    }
    
在LifecycleOwner中添加start方法，开启协程，在Main协程中执行一些ui操作。

### request

    /**
     * execute in io thread pool
     * @param loader http request
     */
    infix fun <T> LifecycleOwner.request(loader: suspend () -> T): Deferred<T> {
        return request(loader)
    }
    
    /**
     * execute in io thread pool
     * @param loader http request
     * @param needAutoCancel need to cancel when activity destroy
     */
    fun <T> LifecycleOwner.request(loader: suspend () -> T, needAutoCancel: Boolean = true): Deferred<T> {
        val deferred = GlobalScope.async(Dispatchers.IO, start = CoroutineStart.LAZY) {
            loader()
        }
        if(needAutoCancel){
            lifecycle.addObserver(CoroutineLifecycleListener(deferred, lifecycle))
        }
        return deferred
    }
    
    internal class CoroutineLifecycleListener(private val deferred: Deferred<*>, private val lifecycle: Lifecycle): LifecycleObserver {
        @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        fun cancelCoroutine() {
            if (deferred.isActive) {
                deferred.cancel()
            }
            lifecycle.removeObserver(this)
        }
    }
    
***request***方法是进行网络请求的方法，我们可以通过一个boolean值来控制是否需要自动取消。如果需要，我们则为当前LifecycleOwner增加一个LifecycleObserver，在onDestroy的时候自动取消请求即可。  
***loader***参数为retrofit的网络请求方法，将其放入协程提供的io线程中进行操作，返回一个deferred对象，deferred对象可以用来控制自动取消。

### then

    /**
     * execute in main thread
     * @param onSuccess callback for onSuccess
     * @param onError callback for onError
     * @param onComplete callback for onComplete
     */
    fun <T> Deferred<T>.then(onSuccess: suspend (T) -> Unit, onError: suspend (String) -> Unit, onComplete: (() -> Unit)? = null): Job {
        return GlobalScope.launch(context = Main) {
            try {
                val result = this@then.await()
                onSuccess(result)
            } catch (e: Exception) {
                e.printStackTrace()
                when (e) {
                    is UnknownHostException -> onError("network is error!")
                    is TimeoutException -> onError("network is error!")
                    is SocketTimeoutException -> onError("network is error!")
                    else -> onError("network is error!")
                }
            }finally {
                onComplete?.invoke()
            }
        }
    }

then方法是对请求结果的处理，方法内传入onSuccess、onError、onCompete的回调，onComplete为可选参数，默认为null。  
为Deferred加入then方法，我们在最外层开启主协程，然后在主协程内首先调用await方法，其实也就是将上面request中的defereed进行网络请求操作，而defereed是在子协程中，所以不会有线程阻塞的问题。  
请求完成后，我们就可以对回调结果进行一系列处理。  

## Presenter
因为文章是基于常见对mvp架构形式，所以我们将网络请求方法对位置放于Presenter中，上面封装的网络请求方法是放在Lifecycle中，而Presenter是没有实现Lifecycle方法的，因此我们需要进行一些封装，这里我直接将其放在MainPresenter中，实际开发的时候可以写在BasePresenter中，防止每次都要写一遍。  


    class MainPresenter : DefaultLifecycleObserver, LifecycleOwner {
        private val TAG = "MainPresenter"
    
        private var lifecycleRegistry: LifecycleRegistry = LifecycleRegistry(this)
    
        override fun getLifecycle(): Lifecycle = lifecycleRegistry
    
        override fun onDestroy(owner: LifecycleOwner) {
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
            lifecycle.removeObserver(this)
        }
    }
    
    class MainActivity : AppCompatActivity() {
    
        private val mainPresenter: MainPresenter by lazy { MainPresenter() }
    
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            setContentView(R.layout.activity_main)
            lifecycle.addObserver(mainPresenter)
        }
    
        override fun onDestroy() {
            super.onDestroy()
            lifecycle.removeObserver(mainPresenter)
        }
    }
    
我们将Presenter同时定义为**LifecycleOwner**和**LifecycleObserver**。  
定义为LifecycleObserver，是为了让presenter可以对Activity的生命周期进行监听。  
定义为LifecycleOwner是为了在其中进行网络请求，我们在重写的onDestroy内加入：  

           lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
这样Activity进行onDestroy的时候，就会走之前定义的**CoroutineLifecycleListener**中**cancelCoroutine**方法，去取消协程。

## 完整的调用过程

    class MainActivity : AppCompatActivity() {
    
        private val mainPresenter: MainPresenter by lazy { MainPresenter() }
    
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            setContentView(R.layout.activity_main)
            lifecycle.addObserver(mainPresenter)
            mainPresenter.doHttpRequest()
        }
    
        override fun onDestroy() {
            super.onDestroy()
            lifecycle.removeObserver(mainPresenter)
        }
    }
    
    class MainPresenter : DefaultLifecycleObserver, LifecycleOwner {
        private val TAG = "MainPresenter"
    
        private var lifecycleRegistry: LifecycleRegistry = LifecycleRegistry(this)
    
        override fun getLifecycle(): Lifecycle = lifecycleRegistry
    
        override fun onDestroy(owner: LifecycleOwner) {
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
            lifecycle.removeObserver(this)
        }
    
        /**
         * 打印结果如下：
         *
         * MainPresenter: start doHttpRequest:currentThreadName:main
         * MainPresenter: request doHttpRequest:currentThreadName:DefaultDispatcher-worker-2
         * MainPresenter: onSuccess doHttpRequest:currentThreadName:main
         * MainPresenter: UserBean(login=null, id=61097549, node_id=MDEwOlJlcG9zaXRvcnk2MTA5NzU0OQ==, avatar_url=null, gravatar_id=null, url=https://api.github.com/repos/JavaNoober/Album, html_url=https://github.com/JavaNoober/Album, followers_url=null, following_url=null, gists_url=null, starred_url=null, subscriptions_url=null, organizations_url=null, repos_url=null, events_url=https://api.github.com/repos/JavaNoober/Album/events, received_events_url=null, type=null, site_admin=false, name=Album, company=null, blog=null, location=null, email=null, hireable=null, bio=null, public_repos=0, public_gists=0, followers=0, following=0, created_at=2016-06-14T06:28:05Z, updated_at=2016-06-14T06:40:26Z)
         * MainPresenter: onComplete doHttpRequest:currentThreadName:main
         */
        fun doHttpRequest() {
            start {
                Log.e(TAG, "start doHttpRequest:currentThreadName:${Thread.currentThread().name}")
            }.request {
                Log.e(TAG, "request doHttpRequest:currentThreadName:${Thread.currentThread().name}")
                RetrofitHelper.getApi().getUserInfo()
            }.then(onSuccess = {
                Log.e(TAG, "onSuccess doHttpRequest:currentThreadName:${Thread.currentThread().name}")
                Log.e(TAG, it[0].toString())
            }, onError = {
                Log.e(TAG, "onError doHttpRequest:currentThreadName:${Thread.currentThread().name}")
            }) {
                Log.e(TAG, "onComplete doHttpRequest:currentThreadName:${Thread.currentThread().name}")
            }
        }
    }
    
上述封装完之后，就可以进行网络请求了。实际开发过程中，可以对其进行进一步封装。  

## 总结
所有的核心代码加起来也不过100行左右，但是就完成了一个安全轻量简便的网络请求操作。因为在这里使用了Lifecycle，其实后续也可以考虑使用配合LiveData，这样更可以达到其他框架难以实现的对数据生命周期的处理。  
完整的代码已上传至github([CoroutinesHttp](https://github.com/JavaNoober/CoroutinesHttp))，欢迎大家提出更好的建议方法。  


## 前言
[上篇文章（DSL形式的基于retorfit、协程的网络请求封装）](https://juejin.im/post/5d48e757f265da03bf0f29a3)介绍了，如何基于retorfit、协程去开发一个dsl形式的网络请求，但是封装完后的写法并不足够DSL，有童鞋表示看起来还是如rxjava一样的链式请求而已。接下来便封装一个标准的DSL网络请求方式。  

DSL是Domain-specific language(领域特定语言)的缩写，维基百科的定义是指的是专注于某个应用程序领域的计算机语言。  
这种说法看起来很抽象，其实大家很常用的gradle就是DSL最常用体现，可以看一下android project中的build.gradle:  
![image](https://raw.githubusercontent.com/JavaNoober/CoroutinesHttp/master/gradleDSL.png)

android{}, dependencies{}这种都是DSL的表现形式，相对于传统的写法更加简洁、表现内容更加明显，如配置文件般的去执行方法，这也是为什么推荐DSL写法的原因。

以下封装的标准DSL请求方式如下：

![image](https://raw.githubusercontent.com/JavaNoober/CoroutinesHttp/master/HTTPDSL.png)

对比之后我们发现可以说是和gradle基本一样，接下来就展示如何封装。
    
## request2
### 分析
- 首先请求是放在一个request对象内
- request内包含多个方法loader、start、onSuccess等，作用也很明显就是，不再过多阐述

### 构建request

    class Request<T> {
        lateinit var loader: suspend () -> T
    
        var start: (() -> Unit)? = null
    
        var onSuccess: ((T) -> Unit)? = null
    
        var onError: ((String) -> Unit)? = null
    
        var onComplete: (() -> Unit)? = null
    
        var addLifecycle: LifecycleOwner? = null
    
        fun request() {
            request(addLifecycle)
        }
    
        fun request(addLifecycle: LifecycleOwner?) {
    
            GlobalScope.launch(context = Dispatchers.Main) {
    
                start?.invoke()
                try {
                    val deferred = GlobalScope.async(Dispatchers.IO, start = CoroutineStart.LAZY) {
                        loader()
                    }
                    addLifecycle?.apply { lifecycle.addObserver(CoroutineLifecycleListener(deferred, lifecycle)) }
                    val result = deferred.await()
                    onSuccess?.invoke(result)
                } catch (e: Exception) {
                    e.printStackTrace()
                    when (e) {
                        is UnknownHostException -> onError?.invoke("network is error!")
                        is TimeoutException -> onError?.invoke("network is error!")
                        is SocketTimeoutException -> onError?.invoke("network is error!")
                        else -> onError?.invoke("network is error!")
                    }
                } finally {
                    onComplete?.invoke()
                }
            }
        }
    }
    
Request对象创建好了，里面放入的参数为方法参数，而不是实体类的参数类型，但是现在使用的时候还是需要通过new，才能创建request对象调用其request请求方法，那如何才能直接调用request方法呢，这就需要用到kotlin的扩展函数功能：  

    inline fun <T> request2(buildRequest: Request<T>.() -> Unit) {
        Request<T>().apply(buildRequest).request()
    }
    
    inline fun <T> LifecycleOwner.request2(buildRequest: Request<T>.() -> Unit) {
        Request<T>().apply(buildRequest).request(this)
    }
加入上面两个方法之后，我们就可以直接调用request方法，进行网络请求了：  

        fun doHttpRequest2() {
            request2<List<UserBean>> {
                //addLifecycle 来指定依赖的生命周期的对象
    //            addLifecycle = {}
    
                start = {
                    Log.e(TAG, "start doHttpRequest2:currentThreadName:${Thread.currentThread().name}")
                }
    
                loader = {
                    Log.e(TAG, "request doHttpRequest2:currentThreadName:${Thread.currentThread().name}")
                    RetrofitHelper.getApi().getUserInfo()
                }
    
                onSuccess = {
                    Log.e(TAG, "onSuccess doHttpRequest2:currentThreadName:${Thread.currentThread().name}")
                    Log.e(TAG, it[0].toString())
                }
    
                onError = {
                    Log.e(TAG, "onError doHttpRequest2:currentThreadName:${Thread.currentThread().name}")
                }
    
                onComplete = {
                    Log.e(TAG, "onComplete doHttpRequest2:currentThreadName:${Thread.currentThread().name}")
                }
            }
        }
        
加入扩展函数之后，这样看起来基本的DSL风格已经有了，但是同gradle对比一看，发现多了一个“=”号，那接下来就想办法去除这个“=”：

修改request如下：  

    class Request<T> {
        private lateinit var loader: suspend () -> T
    
        private var start: (() -> Unit)? = null
    
        private var onSuccess: ((T) -> Unit)? = null
    
        private var onError: ((String) -> Unit)? = null
    
        private var onComplete: (() -> Unit)? = null
    
        private var addLifecycle: LifecycleOwner? = null
    
    
        infix fun loader(loader: suspend () -> T){
            this.loader = loader
        }
    
        infix fun start(start: (() -> Unit)?){
            this.start = start
        }
    
        infix fun onSuccess(onSuccess: ((T) -> Unit)?){
            this.onSuccess = onSuccess
        }
    
        infix fun onError(onError: ((String) -> Unit)?){
            this.onError = onError
        }
    
        infix fun onComplete(onComplete: (() -> Unit)?){
            this.onComplete = onComplete
        }
    
        infix fun addLifecycle(addLifecycle: LifecycleOwner?){
            this.addLifecycle = addLifecycle
        }
    
        fun request() {
            request(addLifecycle)
        }
    
        fun request(addLifecycle: LifecycleOwner?) {
    
            GlobalScope.launch(context = Dispatchers.Main) {
    
                start?.invoke()
                try {
                    val deferred = GlobalScope.async(Dispatchers.IO, start = CoroutineStart.LAZY) {
                        loader()
                    }
                    addLifecycle?.apply { lifecycle.addObserver(CoroutineLifecycleListener(deferred, lifecycle)) }
                    val result = deferred.await()
                    onSuccess?.invoke(result)
                } catch (e: Exception) ~~{~~
                    e.printStackTrace()
                    when (e) {
                        is UnknownHostException -> onError?.invoke("network is error!")
                        is TimeoutException -> onError?.invoke("network is error!")
                        is SocketTimeoutException -> onError?.invoke("network is error!")
                        else -> onError?.invoke("network is error!")
                    }
                } finally {
                    onComplete?.invoke()
                }
            }
        }
    }
    
之所以有“=”，是因为我们把执行的方法当作参数传入的，我们将参数提供一个set方法进行赋值就可以去除“=”，但是调用set方法会出现(),这时我们增加infix字段修饰，这样在set的时候可以直接去除()替换为{}，修改之后调用方法就变成了我们所需要的DSL风格，与gradle如出一辙：  

    /**
     * 打印结果如下：
     *
     * LifecycleMainPresenter: start doHttpRequest:currentThreadName:main
     * LifecycleMainPresenter: request doHttpRequest:currentThreadName:DefaultDispatcher-worker-2
     * LifecycleMainPresenter: onSuccess doHttpRequest:currentThreadName:main
     * LifecycleMainPresenter: UserBean(login=null, id=61097549, node_id=MDEwOlJlcG9zaXRvcnk2MTA5NzU0OQ==, avatar_url=null, gravatar_id=null, url=https://api.github.com/repos/JavaNoober/Album, html_url=https://github.com/JavaNoober/Album, followers_url=null, following_url=null, gists_url=null, starred_url=null, subscriptions_url=null, organizations_url=null, repos_url=null, events_url=https://api.github.com/repos/JavaNoober/Album/events, received_events_url=null, type=null, site_admin=false, name=Album, company=null, blog=null, location=null, email=null, hireable=null, bio=null, public_repos=0, public_gists=0, followers=0, following=0, created_at=2016-06-14T06:28:05Z, updated_at=2016-06-14T06:40:26Z)
     * LifecycleMainPresenter: onComplete doHttpRequest:currentThreadName:main
     */
    fun doHttpRequest2() {
        request2<List<UserBean>> {
            start {
                Log.e(TAG, "start doHttpRequest2:currentThreadName:${Thread.currentThread().name}")
            }

            loader {
                Log.e(TAG, "request doHttpRequest2:currentThreadName:${Thread.currentThread().name}")
                RetrofitHelper.getApi().getUserInfo()
            }

            onSuccess {
                Log.e(TAG, "onSuccess doHttpRequest2:currentThreadName:${Thread.currentThread().name}")
                Log.e(TAG, it[0].toString())
            }

            onError {
                Log.e(TAG, "onError doHttpRequest2:currentThreadName:${Thread.currentThread().name}")
            }

            onComplete {
                Log.e(TAG, "onComplete doHttpRequest2:currentThreadName:${Thread.currentThread().name}")
            }
        }
    }

## 总结
[上篇文章（DSL形式的基于retrofit、协程的网络请求封装）](https://juejin.im/post/5d48e757f265da03bf0f29a3)主要是介绍了如何去封装协程+retrofit的网络请求，这篇则是更加***侧重于封装DSL风格***请求，完整的代码已上传至github([CoroutinesHttp](https://github.com/JavaNoober/CoroutinesHttp))，欢迎大家提出更好的建议方法。  

像使用gradle一样，在kotlin中进行网络请求