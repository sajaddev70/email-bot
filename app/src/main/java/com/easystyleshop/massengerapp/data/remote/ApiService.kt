package com.easystyleshop.massengerapp.data.remote

import com.easystyleshop.massengerapp.data.model.ApiResponse
import com.easystyleshop.massengerapp.data.model.Message
import com.easystyleshop.massengerapp.data.model.User
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

// رابط برای فراخوانی‌های API
interface ApiService {
    @POST("register")
    suspend fun register(
        @Query("phoneNumber") phoneNumber: String,
        @Query("password") password: String,
        @Query("email") email: String
    ): ApiResponse<String>

    @GET("me")
    suspend fun getUserProfile(
        @Header("Authorization") token: String
    ): ApiResponse<User>

    @POST("message")
    suspend fun sendMessage(
        @Header("Authorization") token: String,
        @Query("email") email: String,
        @Query("subject") subject: String,
        @Query("content") content: String,
        @Query("phoneNumber") phoneNumber: String
    ): ApiResponse<Message>

    @GET("messages/{phoneNumber}")
    suspend fun getMessages(
        @Path("phoneNumber") phoneNumber: String
    ): ApiResponse<Message>

    @FormUrlEncoded
    @POST("send-to-emails")
    suspend fun sendToEmails(
        @Header("Authorization") token: String,
        @Field("subject") subject: String,
        @Field("content") content: String,
        @Field("phoneNumber") phoneNumber: String,
        @Field("emails") emails: List<String>
    ): ApiResponse<Message>
}