package com.yozaky.tareaintegradora.api

import android.os.Build
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

data class StatusResponse(
    val disponible: Boolean
)

data class LoginRequest(
    val pin: String,
    val fcmToken: String,
    val dispositivo: String = "${Build.MANUFACTURER} ${Build.MODEL}"
)

data class LoginResponse(
    val token: String?,
    val error: String?
)

data class NotificationItem(
    val id: String,
    val titulo: String,
    val mensaje: String,
    val fecha_creacion: String
)

interface ApiService {
    @GET("/api/auth/wearos/status")
    suspend fun checkStatus(): Response<StatusResponse>

    @POST("/api/auth/wearos/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @GET("/api/notificaciones")
    suspend fun getNotifications(
        @Header("Authorization") token: String,
        @Query("limite") limit: Int = 5
    ): Response<List<NotificationItem>>
}
