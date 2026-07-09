package com.easystyleshop.massengerapp.data.model

data class FormModel(
    val id: Long = 0,
    val telegramId: Long = 0,
    val name: String,
    val firstName: String? = null,
    val lastName: String? = null,
    val userName: String? = null,
    val languageCode: String? = null,
    val phone: String,
    val age: String,
    val location: String,
    val job: String,
    val dateStart: String,
    val skills: String
){
    var persianDate=""
}
