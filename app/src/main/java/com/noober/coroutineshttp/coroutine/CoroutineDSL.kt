package com.noober.coroutineshttp.coroutine

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.OnLifecycleEvent
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.Main
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeoutException


internal class CoroutineLifecycleListener(private val deferred: Deferred<*>, private val lifecycle: Lifecycle): LifecycleObserver {
    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    fun cancelCoroutine() {
        if (deferred.isActive) {
            deferred.cancel()
        }
        lifecycle.removeObserver(this)
    }
}

/**
 * execute in main thread
 */
infix fun LifecycleOwner.start(start: (() -> Unit)): LifecycleOwner{
    GlobalScope.launch(Main) {
        start()
    }
    return this
}

/**
 * execute in io thread pool
 */
infix fun <T> LifecycleOwner.request(loader: suspend () -> T): Deferred<T> {
    val deferred = GlobalScope.async(Dispatchers.IO, start = CoroutineStart.LAZY) {
        loader()
    }
    lifecycle.addObserver(CoroutineLifecycleListener(deferred, lifecycle))
    return deferred
}

/**
 * execute in main thread
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