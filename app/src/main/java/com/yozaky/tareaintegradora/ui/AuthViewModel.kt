package com.yozaky.tareaintegradora.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.messaging.FirebaseMessaging
import com.yozaky.tareaintegradora.api.ApiService
import com.yozaky.tareaintegradora.api.LoginRequest
import com.yozaky.tareaintegradora.data.DataStoreManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class AuthViewModel(private val dataStoreManager: DataStoreManager) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState

    private val apiService: ApiService = Retrofit.Builder()
        .baseUrl("http://localhost:3000") // REEMPLAZAR CON TU URL
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(ApiService::class.java)

    init {
        checkToken()
    }

    private fun checkToken() {
        viewModelScope.launch {
            val token = dataStoreManager.token.first()
            _authState.value = if (token != null) AuthState.Authenticated else AuthState.Unauthenticated
        }
    }

    fun login(pin: String) {
        viewModelScope.launch {
            try {
                val fcmToken = try {
                    FirebaseMessaging.getInstance().token.await()
                } catch (e: Exception) {
                    "token_error"
                }
                val response = apiService.login(LoginRequest(pin, fcmToken))
                
                if (response.isSuccessful && response.body()?.token != null) {
                    dataStoreManager.saveToken(response.body()!!.token!!)
                    _authState.value = AuthState.Authenticated
                } else {
                    _authState.value = AuthState.Error("PIN Incorrecto")
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error("Error de conexión")
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            dataStoreManager.clearToken()
            _authState.value = AuthState.Unauthenticated
        }
    }
}

sealed class AuthState {
    object Loading : AuthState()
    object Authenticated : AuthState()
    object Unauthenticated : AuthState()
    data class Error(val message: String) : AuthState()
}
