package com.chatapp.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import com.chatapp.data.model.Message

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE chatId = :chatId ORDER BY timestamp ASC")
    fun getMessagesForChat(chatId: String): Flow<List<Message>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: Message)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<Message>)

    @Query("UPDATE messages SET status = :status WHERE messageId = :messageId")
    suspend fun updateMessageStatus(messageId: String, status: String) // Use String for enum or TypeConverter

    @Query("DELETE FROM messages WHERE chatId = :chatId")
    suspend fun deleteChatHistory(chatId: String)
}
