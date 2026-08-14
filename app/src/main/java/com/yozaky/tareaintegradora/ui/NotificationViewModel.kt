package com.yozaky.tareaintegradora.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yozaky.tareaintegradora.api.NotificationItem
import com.yozaky.tareaintegradora.api.RetrofitClient
import com.yozaky.tareaintegradora.data.DataStoreManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class NotificationViewModel(private val dataStoreManager: DataStoreManager) : ViewModel() {

    private val _notificationsState = MutableStateFlow<NotificationsState>(NotificationsState.Loading)
    val notificationsState: StateFlow<NotificationsState> = _notificationsState

    private val apiService = RetrofitClient.instance

    fun fetchNotifications() {
        viewModelScope.launch {
            _notificationsState.value = NotificationsState.Loading
            try {
                val token = dataStoreManager.token.first()
                if (token != null) {
                    val response = apiService.getNotifications("Bearer $token")
                    if (response.isSuccessful) {
                        val notifications = response.body() ?: emptyList()
                        if (notifications.isEmpty()) {
                            _notificationsState.value = NotificationsState.Empty
                        } else {
                            _notificationsState.value = NotificationsState.Success(notifications)
                        }
                    } else {
                        _notificationsState.value = NotificationsState.Error("Error al obtener notificaciones")
                    }
                } else {
                    _notificationsState.value = NotificationsState.Error("No hay sesión activa")
                }
            } catch (e: Exception) {
                _notificationsState.value = NotificationsState.Error("Error de red: ${e.message}")
            }
        }
    }
}

sealed class NotificationsState {
    object Loading : NotificationsState()
    object Empty : NotificationsState()
    data class Success(val notifications: List<NotificationItem>) : NotificationsState()
    data class Error(val message: String) : NotificationsState()
}
