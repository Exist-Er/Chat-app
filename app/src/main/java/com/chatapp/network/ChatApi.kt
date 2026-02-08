package com.chatapp.network

import com.chatapp.data.model.Message
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ChatApi {

    @POST("/api/register")
    suspend fun register(@Body registration: UserRegistrationRequest): Response<UserResponse>

    @GET("/api/users/{userId}/public-key")
    suspend fun getPublicKey(@Path("userId") userId: String): Response<PublicKeyResponse>

    @POST("/api/events")
    suspend fun sendEvent(@Body event: BackendEvent): Response<EventResponse>
    
    // Helper models for network calls
}

data class UserRegistrationRequest(
    val google_id_token: String,
    val public_key: String,
    val display_name: String?
)

data class UserResponse(
    val user_id: String,
    val email: String,
    val public_key: String
)

data class PublicKeyResponse(
    val user_id: String,
    val public_key: String
)

data class BackendEvent(
    val event_id: String,
    val recipient_id: String,
    val sender_id: String?,
    val event_type: String,
    val sequence: Long,
    val timestamp: Long,
    val metadata: Map<String, Any>,
    val encrypted_payload: String
)

data class EventResponse(
    val success: Boolean,
    val event_id: String,
    val delivered: Boolean
)
