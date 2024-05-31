package com.example.timeworthapp.api

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface GeoCoderService {
    @GET("search")
    fun getCurrentCoords(
        @Query("q") query: String,
        @Query("api_key") apiKey: String,
    ): Call<List<GeoCoderResponse>>
}