package com.noober.coroutineshttp.retrofit

import retrofit2.http.GET

interface ApiService {

    @GET("users/JavaNoober/repos")
    suspend fun getUserInfo(): List<UserBean>
}