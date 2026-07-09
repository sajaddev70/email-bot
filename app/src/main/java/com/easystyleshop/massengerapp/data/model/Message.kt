package com.easystyleshop.massengerapp.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import androidx.room.Index

@Entity(
    tableName = "messages",
    foreignKeys = [ForeignKey(
        entity = User::class,
        parentColumns = ["id"],
        childColumns = ["userId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index(value = ["userId"])]
)
data class Message(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,        // مقدار اتوماتیک
    val email: String?,
    val subject: String?,
    val content: String?,
    var userId: Int
)

