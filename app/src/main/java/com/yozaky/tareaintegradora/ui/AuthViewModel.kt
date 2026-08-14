package com.yozaky.tareaintegradora.ui

import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.messaging.FirebaseMessaging
import com.yozaky.tareaintegradora.api.ApiService
import com.yozaky.tareaintegradora.api.LoginRequest
import com.yozaky.tareaintegradora.api.RetrofitClient
import com.yozaky.tareaintegradora.data.DataStoreManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AuthViewModel(private val dataStoreManager: DataStoreManager) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState

    private val _serviceStatus = MutableStateFlow<ServiceStatus>(ServiceStatus.Checking)
    val serviceStatus: StateFlow<ServiceStatus> = _serviceStatus

    private val apiService: ApiService = RetrofitClient.instance

    init {
        checkStatus()
        checkToken()
    }

    fun checkStatus() {
        viewModelScope.launch {
            _serviceStatus.value = ServiceStatus.Checking
            try {
                val response = apiService.checkStatus()
                if (response.isSuccessful && response.body()?.disponible == true) {
                    _serviceStatus.value = ServiceStatus.Available
                } else {
                    _serviceStatus.value = ServiceStatus.Unavailable
                }
            } catch (e: Exception) {
                _serviceStatus.value = ServiceStatus.Unavailable
            }
        }
    }

    private fun checkToken() {
        viewModelScope.launch {
            val token = dataStoreManager.token.first()
            _authState.value = if (token != null) AuthState.Authenticated else AuthState.Unauthenticated
        }
    }

    fun login(pin: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val fcmToken = try {
                    FirebaseMessaging.getInstance().token.await()
                } catch (e: Exception) {
                    "token_error"
                }
                
                val dispositivo = "${Build.MANUFACTURER} ${Build.MODEL}"
                val response = apiService.login(LoginRequest(pin, fcmToken, dispositivo))
                
                if (response.isSuccessful && response.body()?.token != null) {
                    dataStoreManager.saveToken(response.body()!!.token!!)
                    _authState.value = AuthState.Authenticated
                } else {
                    val errorMsg = response.body()?.error ?: "PIN Incorrecto"
                    _authState.value = AuthState.Error(errorMsg)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _authState.value = AuthState.Error("Error de conexión: ${e.message}")
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

sealed class ServiceStatus {
    object Checking : ServiceStatus()
    object Available : ServiceStatus()
    object Unavailable : ServiceStatus()
}
