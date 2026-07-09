package com.easystyleshop.massengerapp.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.easystyleshop.massengerapp.data.model.FormModel
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormListScreen(formList: List<FormModel>) {
    var selectedForm by remember { mutableStateOf<FormModel?>(null) }
    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showBottomSheet by remember { mutableStateOf(false) }

    if (showBottomSheet && selectedForm != null) {
        ModalBottomSheet(
            onDismissRequest = {
                showBottomSheet = false
            },
            sheetState = bottomSheetState
        ) {
            BottomSheetContent(
                form = selectedForm!!,
                onClose = {
                    showBottomSheet = false
                }
            )
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(8.dp)
    ) {
        itemsIndexed(formList) { _, form ->
            form.persianDate=formatDateToPersian(form.dateStart)
            FormItem(
                form = form,
                onClick = {
                    selectedForm = form
                    showBottomSheet = true
                }
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
fun FormItem(form: FormModel, onClick: () -> Unit) {
    val formattedDate = remember(form.dateStart) { formatDateToPersian(form.dateStart) }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = "نام کامل: ${form.name}", style = MaterialTheme.typography.titleMedium)
            if (!form.firstName.isNullOrEmpty() || !form.lastName.isNullOrEmpty()) {
                Text(text = "نام: ${form.firstName ?: ""} ${form.lastName ?: ""}")
            }
            if (!form.userName.isNullOrEmpty()) {
                Text(text = "نام کاربری: @${form.userName}")
            }
            Text(text = "شماره تلگرام: ${form.telegramId}")
            Text(text = "زبان: ${form.languageCode ?: "ناشناخته"}")
            Text(text = "تلفن: ${form.phone}")
            Text(text = "سن: ${form.age}")
            Text(text = "محل: ${form.location}")
            Text(text = "شغل: ${form.job}")
            Text(text = "مهارت‌ها: ${form.skills}")
            Text(text = "تاریخ شروع: $formattedDate")
        }
    }
}

@Composable
fun BottomSheetContent(form: FormModel, onClose: () -> Unit) {
    val formattedDate = remember(form.dateStart) { formatDateToPersian(form.dateStart) }

    Column(modifier = Modifier.padding(16.dp)) {
        Text(text = "جزئیات فرم", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "ID: ${form.id}")
        Text(text = "Telegram ID: ${form.telegramId}")
        Text(text = "نام کامل: ${form.name}")
        Text(text = "نام: ${form.firstName ?: ""} ${form.lastName ?: ""}")
        Text(text = "نام کاربری: @${form.userName ?: ""}")
        Text(text = "زبان: ${form.languageCode ?: "ناشناخته"}")
        Text(text = "تلفن: ${form.phone}")
        Text(text = "سن: ${form.age}")
        Text(text = "محل: ${form.location}")
        Text(text = "شغل: ${form.job}")
        Text(text = "مهارت‌ها: ${form.skills}")
        Text(text = "تاریخ شروع: $formattedDate")

        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = { onClose() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "متوجه شدم")
        }
    }
}

fun formatDateToPersian(utcDateTime: String): String {
    return try {
        val inputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        val utcLocalDateTime = LocalDateTime.parse(utcDateTime, inputFormatter)
        val utcZoned = utcLocalDateTime.atZone(ZoneId.of("UTC"))
        val tehranZoned = utcZoned.withZoneSameInstant(ZoneId.of("Asia/Tehran"))

        val gYear = tehranZoned.year
        val gMonth = tehranZoned.monthValue
        val gDay = tehranZoned.dayOfMonth
        val (hourStr, minuteStr, thirdStr) = extractTimeParts(utcDateTime)
        val hour = hourStr.toInt()
        val minute = minuteStr.toInt()
        val third = thirdStr.toInt()

        val timeStr = "%02d:%02d:%02d".format(hour, minute, third).toPersianDigits()


        val (jy, jm, jd) = gregorianToJalali(gYear, gMonth, gDay)

        val dateStr = "%04d/%02d/%02d".format(jy, jm, jd).toPersianDigits()

        "$dateStr - ساعت $timeStr"
    } catch (e: Exception) {
        "نامعتبر"
    }
}

fun extractTimeParts(dateTime: String): Triple<String, String, String> {
    val timePart = dateTime.split(" ")[1] // "10:42:56"
    val (hour, minute, second) = timePart.split(":")
    return Triple(hour, minute, second)
}


fun String.toPersianDigits(): String {
    val persianDigits = mapOf(
        '0' to '۰', '1' to '۱', '2' to '۲', '3' to '۳',
        '4' to '۴', '5' to '۵', '6' to '۶', '7' to '۷',
        '8' to '۸', '9' to '۹'
    )
    return this.map { persianDigits[it] ?: it }.joinToString("")
}



// تابع تبدیل تاریخ میلادی به شمسی بدون کتابخانه
fun gregorianToJalali(gy: Int, gm: Int, gd: Int): Triple<Int, Int, Int> {
    val gdm = intArrayOf(31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
    val jdm = intArrayOf(31, 31, 31, 31, 31, 31, 30, 30, 30, 30, 30, 29)

    var gy2 = gy - 1600
    var gm2 = gm - 1
    var gd2 = gd - 1

    var gDayNo = 365 * gy2 + (gy2 + 3) / 4 - (gy2 + 99) / 100 + (gy2 + 399) / 400
    for (i in 0 until gm2) gDayNo += gdm[i]
    if (gm2 > 1 && isLeapGregorian(gy)) gDayNo += 1
    gDayNo += gd2

    var jDayNo = gDayNo - 79

    val jNp = jDayNo / 12053
    jDayNo %= 12053

    var jy = 979 + 33 * jNp + 4 * (jDayNo / 1461)
    jDayNo %= 1461

    if (jDayNo >= 366) {
        jy += (jDayNo - 1) / 365
        jDayNo = (jDayNo - 1) % 365
    }

    var jm = 0
    var jd = 0
    for (i in 0..11) {
        if (jDayNo < jdm[i]) {
            jm = i + 1
            jd = jDayNo + 1
            break
        }
        jDayNo -= jdm[i]
    }

    return Triple(jy, jm, jd)
}

fun isLeapGregorian(year: Int): Boolean {
    return (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)
}
