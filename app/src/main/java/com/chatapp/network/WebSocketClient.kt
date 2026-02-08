package com.chatapp.network

import android.util.Log
import com.chatapp.data.model.Message
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class WebSocketClient(
    private val client: OkHttpClient,
    private val scope: CoroutineScope
) {
    private var webSocket: WebSocket? = null
    private val _incomingEvents = Channel<BackendEvent>(Channel.BUFFERED)
    val incomingEvents: Flow<BackendEvent> = _incomingEvents.receiveAsFlow()
    
    // Connection state?

    fun connect(userId: String) {
        webSocket?.close(1000, "Reconnecting")
        val request = Request.Builder()
            .url("ws://192.168.69.102:8080/ws/$userId")
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d("WebSocket", "Connected")
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    Log.d("WebSocket", "Message: $text")
                    val gson = com.google.gson.Gson()
                    val event = gson.fromJson(text, BackendEvent::class.java)
                    scope.launch { _incomingEvents.send(event) }
                } catch (e: Exception) {
                    Log.e("WebSocket", "Error parsing message", e)
                }
            }
            
            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d("WebSocket", "Closed: $code / $reason")
            }
            
            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e("WebSocket", "Failure", t)
                // Reconnect logic needed here
            }
        })
    }

    fun sendAck(eventId: String, recipientId: String) {
        val json = JSONObject().apply {
            put("type", "ACK")
            put("event_id", eventId)
            put("recipient_id", recipientId)
        }
        webSocket?.send(json.toString())
    }

    fun close() {
        webSocket?.close(1000, "User logout")
    }
}
