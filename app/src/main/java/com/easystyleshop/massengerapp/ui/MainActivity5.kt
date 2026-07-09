package com.easystyleshop.massengerapp.ui

import android.content.Context
import android.os.Bundle
import android.os.Environment
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.easystyleshop.massengerapp.data.model.TelegramUser
import com.easystyleshop.massengerapp.ui.theme.MessengerAppTheme
import com.easystyleshop.massengerapp.util.createTelegramUserExcel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

// Retrofit API
interface TelegramApiService {
    @GET("api/telegram/users")
    suspend fun getTelegramUsers(): List<TelegramUser>
}

class MainActivity5 : ComponentActivity() {
    private val userList = mutableStateListOf<TelegramUser>()

    private val api: TelegramApiService by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()
        Retrofit.Builder()
            .baseUrl("http://77.90.8.28:8080/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(TelegramApiService::class.java)
    }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MessengerAppTheme {
                val context = LocalContext.current

                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text("Telegram Bot Users") },
                            actions = {
                                IconButton(onClick = { fetchUsers(this@MainActivity5) }) {
                                    Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                                }
                                IconButton(onClick = {
                                    lifecycleScope.launch {
                                        createTelegramUserExcel(context, userList.toList())
                                    }
                                }) {
                                    Icon(Icons.Default.Download, contentDescription = "Save to Excel")
                                }
                            }
                        )
                    }
                ) { padding ->
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .padding(8.dp)
                    ) {
                        items(userList) { user ->
                            user.persianDate=formatIsoDateToPersian(user.startDate ?: "")
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("👤 نام: ${user.firstName.orEmpty()} ${user.lastName.orEmpty()}")
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("🆔 شناسه کاربر: ${user.userId}")
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("🌐 زبان: ${user.languageCode ?: "نامشخص"}")
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("📅 زمان عضویت: ${formatIsoDateToPersian(user.startDate ?: "")}")
                                }
                            }
                        }
                    }
                }
            }
        }

        fetchUsers(this)
    }

    private fun fetchUsers(context: ComponentActivity) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val users = api.getTelegramUsers()
                userList.clear()
                userList.addAll(users)
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        "❌ دریافت لیست کاربران با خطا مواجه شد.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    fun formatIsoDateToPersian(isoDateTime: String): String {
        return try {
            val inputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSS")
            val utcLocalDateTime = LocalDateTime.parse(isoDateTime, inputFormatter)
            val utcZoned = utcLocalDateTime.atZone(ZoneId.of("UTC"))
            val tehranZoned = utcZoned.withZoneSameInstant(ZoneId.of("Asia/Tehran"))

            val gYear = tehranZoned.year
            val gMonth = tehranZoned.monthValue
            val gDay = tehranZoned.dayOfMonth
            val hour = tehranZoned.hour
            val minute = tehranZoned.minute
            val second = tehranZoned.second

            val (jy, jm, jd) = gregorianToJalali(gYear, gMonth, gDay)

            val timeStr = "%02d:%02d:%02d".format(hour, minute, second).toPersianDigits()
            val dateStr = "%04d/%02d/%02d".format(jy, jm, jd).toPersianDigits()

            "$dateStr - ساعت $timeStr"
        } catch (e: Exception) {
            "نامعتبر"
        }
    }

    private fun gregorianToJalali(gy: Int, gm: Int, gd: Int): Triple<Int, Int, Int> {
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

    private fun isLeapGregorian(year: Int): Boolean {
        return (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)
    }

    private fun String.toPersianDigits(): String {
        val persianDigits = listOf('۰','۱','۲','۳','۴','۵','۶','۷','۸','۹')
        val sb = StringBuilder()
        for (ch in this) {
            if (ch in '0'..'9') {
                sb.append(persianDigits[ch - '0'])
            } else {
                sb.append(ch)
            }
        }
        return sb.toString()
    }
}
