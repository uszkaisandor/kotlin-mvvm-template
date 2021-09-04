package com.codinginflow.mvvmnewsapp.api

import retrofit2.http.GET
import retrofit2.http.Query

interface NewsApi {

    companion object {
        const val BASE_URL = "https://newsapi.org/v2/"
    }

    //@Headers("Non-Authenticated: true")
    @GET("top-headlines?country=hu&pageSize=100")
    suspend fun getBreakingNews(): NewsResponse

    @GET("everything")
    suspend fun searchNews(
        @Query("q") query: String,
        @Query("page") page: Int,
        @Query("pageSize") pageSize: Int
    ): NewsResponse
}