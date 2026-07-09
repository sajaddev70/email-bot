package com.easystyleshop.massengerapp.data.model

data class TelegramUser(
    val userId: Long,
    val username: String?,
    val firstName: String?,
    val lastName: String?,
    val languageCode: String?,
    val startDate: String?
){
    var persianDate=""
}
