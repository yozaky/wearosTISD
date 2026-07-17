package com.yozaky.tareaintegradora.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

data class LoginRequest(
    val pin: String,
    val fcmToken: String,
    val dispositivo: String = "WearOS_Watch"
)

data class LoginResponse(
    val token: String?,
    val error: String?
)

interface ApiService {
    @POST("/api/auth/wearos/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>
}
