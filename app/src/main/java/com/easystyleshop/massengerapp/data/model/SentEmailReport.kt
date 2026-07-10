package com.easystyleshop.massengerapp.data.model

data class SentEmailReport(
    val sender: String,
    val recipient: String,
    val subject: String,
    val body: String,
    val sentAtGregorian: String
)
