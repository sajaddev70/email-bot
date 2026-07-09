package com.easystyleshop.massengerapp.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

// مدل کاربر برای Room و Retrofit
@Entity(tableName = "users")
data class User(
    @PrimaryKey(autoGenerate = true) val id: Int?=1,
    val phoneNumber: String?,
    val password: String?,
    val email: String?,
    var token: String?
)