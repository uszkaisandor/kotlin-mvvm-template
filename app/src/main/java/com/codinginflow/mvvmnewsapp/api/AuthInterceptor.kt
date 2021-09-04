package com.codinginflow.mvvmnewsapp.api

import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor : Interceptor {

    var token: String = ""

    override fun intercept(chain: Interceptor.Chain): Response {
        var request = chain.request()

        if (token.isNotEmpty() && request.headers("Non-Authenticated").isEmpty()) {
            request = request.newBuilder()
                .addHeader("X-Api-Key", token)
                .build()
        }

        return chain.proceed(request)
    }
}