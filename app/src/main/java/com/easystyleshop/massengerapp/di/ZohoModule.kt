package com.easystyleshop.massengerapp.di

import android.content.Context
import com.chuckerteam.chucker.api.ChuckerInterceptor
import com.easystyleshop.massengerapp.data.remote.ZohoApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ZohoModule {

    @Provides
    @Singleton
    fun provideZohoOkHttpClient(@ApplicationContext context: Context): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        return OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(logging)
            .addInterceptor(ChuckerInterceptor.Builder(context).build()) // دیباگر تصویری
            .build()
    }

    @Provides
    @Singleton
    fun provideZohoRetrofit(client: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://mail.zoho.eu/zm/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideZohoApiService(retrofit: Retrofit): ZohoApiService {
        return retrofit.create(ZohoApiService::class.java)
    }
}
