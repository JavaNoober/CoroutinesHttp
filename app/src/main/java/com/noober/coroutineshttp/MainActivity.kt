package com.noober.coroutineshttp

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private val TAG = "MainActivity"

    private val lifecycleMainPresenter: LifecycleMainPresenter by lazy { LifecycleMainPresenter() }
    private val mainPresenter: MainPresenter by lazy { MainPresenter() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        lifecycle.addObserver(lifecycleMainPresenter)
        lifecycleMainPresenter.doHttpRequest()
        mainPresenter.doHttpRequest3()
//        Test().doHttp()


        // do first: main
        // do async: DefaultDispatcher-worker-5
        // do end: main
        GlobalScope.launch(Dispatchers.Main) {
            println("do first: ${Thread.currentThread().name}")
            GlobalScope.async(Dispatchers.Default) {
                println("do async: ${Thread.currentThread().name}")
            }.await()
            println("do end: ${Thread.currentThread().name}")
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        lifecycle.removeObserver(lifecycleMainPresenter)
    }
}
