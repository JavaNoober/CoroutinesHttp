package com.noober.coroutineshttp

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle

class MainActivity : AppCompatActivity() {
    private val TAG = "MainActivity"

    private val mainPresenter: LifecycleMainPresenter by lazy { LifecycleMainPresenter() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        lifecycle.addObserver(mainPresenter)
        mainPresenter.doHttpRequest()
        mainPresenter.doHttpRequest2()
        Test().doHttp()
    }

    override fun onDestroy() {
        super.onDestroy()
        lifecycle.removeObserver(mainPresenter)
    }
}
