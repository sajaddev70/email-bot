package com.easystyleshop.massengerapp.repository

import com.easystyleshop.massengerapp.data.local.AppDatabase
import com.easystyleshop.massengerapp.data.model.ApiResponse
import com.easystyleshop.massengerapp.data.model.User
import com.easystyleshop.massengerapp.data.remote.ApiService
import com.easystyleshop.massengerapp.data.remote.ZohoApiService
import com.easystyleshop.massengerapp.util.Resource

// ریپازیتوری برای مدیریت عملیات کاربر
class UserRepository(
    private val apiService: ApiService,
    private val zohoApiService: ZohoApiService,
    private val db: AppDatabase
) {
    suspend fun register(phoneNumber: String, password: String, email: String): Resource<String> {
        return try {
            val response = apiService.register(phoneNumber, password, email)
            if (response.isHasError) {
                Resource.Error(response.message)
            } else {
                Resource.Success(response.message)
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "خطا در ثبت‌نام")
        }
    }

    suspend fun getUserProfile(token: String): Resource<User> {
        return try {
            val response = apiService.getUserProfile("Bearer $token")
            if (response.isHasError || response.dataList.isEmpty()) {
                Resource.Error(response.message)
            } else {
                val user = response.dataList[0]
                user.token = token
                db.userDao().insert(user)
                Resource.Success(user)
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "خطا در دریافت پروفایل")
        }
    }

    suspend fun sendEmailViaZoho(
        cookie: String,
        csrfToken: String,
        accId: String,
        from: String,
        to: String,
        subject: String,
        content: String
    ): Resource<Unit> {
        return try {
            val response = zohoApiService.sendZohoEmail(
                cookie = cookie,
                csrfToken = csrfToken,
                accId = accId,
                from = from,
                to = to,
                subject = subject,
                content = content
            )
            if (response.contentLength() > 0) {
                Resource.Success(Unit)
            } else {
                Resource.Error("پاسخ خالی از Zoho دریافت شد.")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "خطا در ارسال ایمیل با Zoho")
        }
    }

}