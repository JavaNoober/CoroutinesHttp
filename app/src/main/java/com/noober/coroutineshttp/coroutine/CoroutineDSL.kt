package com.noober.coroutineshttp.coroutine

import android.os.Looper
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.*
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeoutException

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

inline fun <T> request2(buildRequest: Request<T>.() -> Unit) {
    Request<T>().apply(buildRequest).request()
}

inline fun <T> LifecycleOwner.request2(buildRequest: Request<T>.() -> Unit) {
    Request<T>().apply(buildRequest).request(this)
}