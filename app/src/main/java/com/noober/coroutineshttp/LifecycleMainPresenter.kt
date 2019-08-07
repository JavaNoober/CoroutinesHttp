package com.noober.coroutineshttp

import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import com.noober.coroutineshttp.coroutine.request
import com.noober.coroutineshttp.coroutine.request2
import com.noober.coroutineshttp.coroutine.start
import com.noober.coroutineshttp.coroutine.then
import com.noober.coroutineshttp.retrofit.RetrofitHelper
import com.noober.coroutineshttp.retrofit.UserBean

class LifecycleMainPresenter : DefaultLifecycleObserver, LifecycleOwner {
    private val TAG = "LifecycleMainPresenter"

    private var lifecycleRegistry: LifecycleRegistry = LifecycleRegistry(this)

    override fun getLifecycle(): Lifecycle = lifecycleRegistry

    override fun onDestroy(owner: LifecycleOwner) {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        lifecycle.removeObserver(this)
    }

    /**
     * 打印结果如下：
     *
     * LifecycleMainPresenter: start doHttpRequest:currentThreadName:main
     * LifecycleMainPresenter: request doHttpRequest:currentThreadName:DefaultDispatcher-worker-2
     * LifecycleMainPresenter: onSuccess doHttpRequest:currentThreadName:main
     * LifecycleMainPresenter: UserBean(login=null, id=61097549, node_id=MDEwOlJlcG9zaXRvcnk2MTA5NzU0OQ==, avatar_url=null, gravatar_id=null, url=https://api.github.com/repos/JavaNoober/Album, html_url=https://github.com/JavaNoober/Album, followers_url=null, following_url=null, gists_url=null, starred_url=null, subscriptions_url=null, organizations_url=null, repos_url=null, events_url=https://api.github.com/repos/JavaNoober/Album/events, received_events_url=null, type=null, site_admin=false, name=Album, company=null, blog=null, location=null, email=null, hireable=null, bio=null, public_repos=0, public_gists=0, followers=0, following=0, created_at=2016-06-14T06:28:05Z, updated_at=2016-06-14T06:40:26Z)
     * LifecycleMainPresenter: onComplete doHttpRequest:currentThreadName:main
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

}