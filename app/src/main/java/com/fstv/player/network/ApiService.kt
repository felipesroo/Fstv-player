package com.fstv.player.network

import com.fstv.player.network.models.CheckResponse
import com.fstv.player.network.models.RegisterResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface ApiService {

    @GET("api/register.php")
    suspend fun registerDevice(
        @Query("mac") mac: String,
        @Query("key") key: String,
        @Query("version") version: String = "1.0"
    ): Response<RegisterResponse>

    @GET("api/check.php")
    suspend fun checkDevice(
        @Query("mac") mac: String,
        @Query("key") key: String
    ): Response<CheckResponse>

}
