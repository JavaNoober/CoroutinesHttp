package com.noober.coroutineshttp.coroutine

import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.*
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeoutException

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
            try {
                start?.invoke()

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