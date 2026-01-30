package com.example.chatapp.features.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chatapp.common.network.NetworkClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {
    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            try {
                val response = NetworkClient.authApi.login(AuthRequest(email = email, password = password))
                if (response.isSuccessful) {
                    _uiState.value = AuthUiState.Success(response.body()?.message ?: "Login Successful")
                } else {
                    _uiState.value = AuthUiState.Error("Login Failed: ${response.code()}")
                }
            } catch (e: Exception) {
                _uiState.value = AuthUiState.Error(e.message ?: "Unknown Error")
            }
        }
    }

    fun signup(username: String, email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            try {
                val response = NetworkClient.authApi.signup(AuthRequest(username, email, password))
                if (response.isSuccessful) {
                    _uiState.value = AuthUiState.Success(response.body()?.message ?: "Signup Successful")
                } else {
                    _uiState.value = AuthUiState.Error("Signup Failed: ${response.code()}")
                }
            } catch (e: Exception) {
                _uiState.value = AuthUiState.Error(e.message ?: "Unknown Error")
            }
        }
    }
}

sealed class AuthUiState {
    object Idle : AuthUiState()
    object Loading : AuthUiState()
    data class Success(val message: String) : AuthUiState()
    data class Error(val error: String) : AuthUiState()
}
