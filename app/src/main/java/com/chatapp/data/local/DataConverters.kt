package com.chatapp.data.local

import androidx.room.TypeConverter
import com.chatapp.data.model.MessageStatus
import com.chatapp.data.model.MessageType
import java.util.Date

class DataConverters {

    @TypeConverter
    fun fromStatus(status: MessageStatus): String = status.name

    @TypeConverter
    fun toStatus(status: String): MessageStatus = MessageStatus.valueOf(status)

    @TypeConverter
    fun fromType(type: MessageType): String = type.name

    @TypeConverter
    fun toType(type: String): MessageType = MessageType.valueOf(type)
    
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? = value?.let { Date(it) }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? = date?.time
}
