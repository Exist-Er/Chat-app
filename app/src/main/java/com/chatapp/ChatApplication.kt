package com.chatapp

import android.app.Application
import com.chatapp.crypto.CryptoManager
import com.chatapp.data.local.AppDatabase
import com.chatapp.data.repository.ChatRepository
import com.chatapp.network.ChatApi
import com.chatapp.network.WebSocketClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class ChatApplication : Application() {
    
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val database by lazy { AppDatabase.getDatabase(this) }
    val cryptoManager by lazy { CryptoManager(this) }
    
    val okHttpClient by lazy { OkHttpClient.Builder().build() }
    
    val retrofit by lazy {
        Retrofit.Builder()
            .baseUrl("http://192.168.69.102:8080")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
    
    val chatApi by lazy { retrofit.create(ChatApi::class.java) }
    
    val webSocketClient by lazy { WebSocketClient(okHttpClient, appScope) }
    
    val repository by lazy {
        ChatRepository(
            messageDao = database.messageDao(),
            groupKeyDao = database.groupKeyDao(),
            api = chatApi,
            cryptoManager = cryptoManager,
            webSocketClient = webSocketClient,
            scope = appScope
        ).apply {
            // Ensure we start observing events as soon as the repository is initialized
            observeIncomingEvents()
        }
    }
}
