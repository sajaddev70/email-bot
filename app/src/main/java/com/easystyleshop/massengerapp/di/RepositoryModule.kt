package com.easystyleshop.massengerapp.di

import com.easystyleshop.massengerapp.data.local.AppDatabase
import com.easystyleshop.massengerapp.data.remote.ApiService
import com.easystyleshop.massengerapp.data.remote.ZohoApiService
import com.easystyleshop.massengerapp.repository.MessageRepository
import com.easystyleshop.massengerapp.repository.UserRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

// ماژول Hilt برای ارائه ریپازیتوری‌ها
@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {
    @Provides
    @Singleton
    fun provideUserRepository(apiService: ApiService,zohoApiService: ZohoApiService, db: AppDatabase): UserRepository {
        return UserRepository(apiService,zohoApiService, db)
    }

    @Provides
    @Singleton
    fun provideMessageRepository(apiService: ApiService, db: AppDatabase): MessageRepository {
        return MessageRepository(apiService, db)
    }
}