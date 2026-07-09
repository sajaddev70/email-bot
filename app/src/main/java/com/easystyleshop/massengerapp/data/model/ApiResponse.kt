package com.easystyleshop.massengerapp.data.model

// مدل پاسخ عمومی از API
data class ApiResponse<T>(
    val dataList: List<T>,
    val status: String,
    val isHasError: Boolean,
    val message: String,
    val totalCount: Int
)