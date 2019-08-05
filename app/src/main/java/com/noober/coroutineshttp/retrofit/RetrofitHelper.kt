package com.noober.coroutineshttp.retrofit

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class RetrofitHelper {


    companion object{
        fun getApi(): ApiService{
            val client = OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)       //连接超时
                .readTimeout(10, TimeUnit.SECONDS)          //读取超时
                .writeTimeout(10, TimeUnit.SECONDS)         //写超时
                .build()
            val retrofit = Retrofit.Builder().client(client).baseUrl("https://api.github.com/")
                .addConverterFactory(GsonConverterFactory.create()).build()
            return retrofit.create(ApiService::class.java)
        }
    }
}