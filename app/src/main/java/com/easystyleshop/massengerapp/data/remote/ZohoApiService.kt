package com.easystyleshop.massengerapp.data.remote

import okhttp3.ResponseBody
import retrofit2.http.*

interface ZohoApiService {
    @FormUrlEncoded
    @POST("send.do")
    suspend fun sendZohoEmail(
        @Header("Accept") accept: String = "application/json",
        @Header("Content-Type") contentType: String = "application/x-www-form-urlencoded; charset=UTF-8",
        @Header("Cookie") cookie: String,
        @Header("User-Agent") userAgent: String = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/134.0.0.0 Safari/537.36",
        @Header("X-ZCSRF-TOKEN") csrfToken: String,
        @Field("accId") accId: String,
        @Field("from") from: String,
        @Field("to") to: String,
        @Field("sendImm") sendImm: Boolean = true,
        @Field("subject") subject: String,
        @Field("content") content: String,
        @Field("smType") smType: Int = 2,
        @Field("charSet") charSet: String = "UTF-8",
        @Field("priority") priority: String = "Medium",
        @Field("originalMode") originalMode: String = "compose"
    ): ResponseBody
}
