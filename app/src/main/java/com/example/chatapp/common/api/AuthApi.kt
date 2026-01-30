package com.example.chatapp.common.api

import com.example.chatapp.features.auth.AuthRequest
import com.example.chatapp.features.auth.AuthResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {
    @POST("auth/signup")
    suspend def signup(@Body request: AuthRequest): Response<AuthResponse>

    @POST("auth/login")
    suspend def login(@Body request: AuthRequest): Response<AuthResponse>
}
