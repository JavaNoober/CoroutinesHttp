package com.noober.coroutineshttp

import android.util.Log
import com.noober.coroutineshttp.coroutine.request2
import com.noober.coroutineshttp.retrofit.RetrofitHelper
import com.noober.coroutineshttp.retrofit.UserBean

class Test{
    private val TAG = "Test"

    fun doHttp(){

        request2<List<UserBean>> {

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
}