package com.easystyleshop.massengerapp.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.easystyleshop.massengerapp.data.model.Message

// DAO برای پیام‌ها
@Dao
interface MessageDao {
    @Insert
    suspend fun insert(message: Message)

    @Query("SELECT * FROM messages WHERE userId = :userId")
    suspend fun getMessagesByUserId(userId: Int): List<Message>
}