package com.example.chatapp.features.auth

data class AuthRequest(
    val username: String? = null,
    val email: String,
    val password: String
)

data class AuthResponse(
    val id: String? = null,
    val message: String,
    val username: String? = null
)
