package com.easystyleshop.massengerapp.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "email_queue")
data class EmailQueueItem(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val email: String,
    val subject: String,
    val content: String,
    val status: String = "PENDING",
    val createdAt: Long = System.currentTimeMillis()
)
