package com.noober.coroutineshttp

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle

class MainActivity : AppCompatActivity() {
    private val TAG = "MainActivity"

    private val lifecycleMainPresenter: LifecycleMainPresenter by lazy { LifecycleMainPresenter() }
    private val mainPresenter: MainPresenter by lazy { MainPresenter() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        lifecycle.addObserver(lifecycleMainPresenter)
        lifecycleMainPresenter.doHttpRequest()
        mainPresenter.doHttpRequest2()
        Test().doHttp()
    }

    override fun onDestroy() {
        super.onDestroy()
        lifecycle.removeObserver(lifecycleMainPresenter)
    }
}
