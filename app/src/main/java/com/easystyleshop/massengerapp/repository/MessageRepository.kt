package com.easystyleshop.massengerapp.repository

import com.easystyleshop.massengerapp.data.local.AppDatabase
import com.easystyleshop.massengerapp.data.model.ApiResponse
import com.easystyleshop.massengerapp.data.model.Message
import com.easystyleshop.massengerapp.data.remote.ApiService
import com.easystyleshop.massengerapp.util.Resource

// ریپازیتوری برای مدیریت عملیات پیام‌ها
class MessageRepository(
    private val apiService: ApiService,
    private val db: AppDatabase
) {
    private val userDao = db.userDao()
    private val messageDao = db.messageDao()

    suspend fun sendMessage(
        token: String,
        email: String,
        subject: String,
        content: String,
        phoneNumber: String
    ): Resource<Message> {
        return try {
            val response =
                apiService.sendMessage("Bearer $token", email, subject, content, phoneNumber)
            if (response.isHasError || response.dataList.isEmpty()) {
                Resource.Error(response.message)
            } else {
                val message = response.dataList[0]
                if (message.userId == 0)
                    message.userId = 1
                // چک کردن وجود کاربر قبل از درج پیام
                val user = userDao.getUserById(message.userId)
                if (user != null) {
                    messageDao.insert(message)
                    Resource.Success(message)
                } else {
                    Resource.Error("کاربر با شناسه ${message.userId} در دیتابیس وجود ندارد!")
                }
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "خطا در ارسال پیام")
        }
    }

    suspend fun getMessages(phoneNumber: String): Resource<List<Message>> {
        return try {
            val response = apiService.getMessages(phoneNumber)
            if (response.isHasError) {
                Resource.Error(response.message)
            } else {
                response.dataList.forEach {
                    // قبل از درج پیام، وجود کاربر را چک کن
                    val user = userDao.getUserById(it.userId)
                    if (user != null) {
                        messageDao.insert(it)
                    }
                }
                Resource.Success(response.dataList)
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "خطا در دریافت پیام‌ها")
        }
    }

    suspend fun sendToEmails(
        token: String,
        subject: String,
        content: String,
        phoneNumber: String,
        emails: List<String>
    ): Resource<List<Message>> {
        return try {
            val response =
                apiService.sendToEmails("Bearer $token", subject, content, phoneNumber, emails)
            if (response.isHasError) {
                Resource.Error(response.message)
            } else {
                response.dataList.forEach {
                    val user = userDao.getUserById(it.userId)
                    if (user != null) {
                        messageDao.insert(it)
                    }
                }
                Resource.Success(response.dataList)
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "خطا در ارسال پیام به ایمیل‌ها")
        }
    }
}
