package com.noober.coroutineshttp

import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import com.noober.coroutineshttp.coroutine.request
import com.noober.coroutineshttp.coroutine.start
import com.noober.coroutineshttp.coroutine.then
import com.noober.coroutineshttp.retrofit.RetrofitHelper

class MainPresenter : DefaultLifecycleObserver, LifecycleOwner {
    private val TAG = "MainPresenter"

    private var lifecycleRegistry: LifecycleRegistry = LifecycleRegistry(this)

    override fun getLifecycle(): Lifecycle = lifecycleRegistry

    override fun onDestroy(owner: LifecycleOwner) {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        lifecycle.removeObserver(this)
    }

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