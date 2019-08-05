package com.noober.coroutineshttp

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle

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
