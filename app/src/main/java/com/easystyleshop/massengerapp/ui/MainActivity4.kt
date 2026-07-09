package com.easystyleshop.massengerapp.ui

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.lifecycleScope
import com.chuckerteam.chucker.api.ChuckerInterceptor
import com.easystyleshop.massengerapp.data.model.FormModel
import com.easystyleshop.massengerapp.ui.screens.FormListScreen
import com.easystyleshop.massengerapp.ui.theme.MessengerAppTheme
import com.easystyleshop.massengerapp.util.createPhoneExcel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET

// API Interface
interface PhoneApiService {
    @GET("api/getAllForms")
    suspend fun getForms(): List<FormModel>
}

class MainActivity4 : ComponentActivity() {
    private val formList = mutableStateListOf<FormModel>()

    private val api: PhoneApiService by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
//            .addInterceptor(ChuckerInterceptor.Builder(this).build())
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl("http://138.124.53.150:8080/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        retrofit.create(PhoneApiService::class.java)
    }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MessengerAppTheme {
                val context = LocalContext.current as ComponentActivity

                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text("ربات کوئیز") },
                            actions = {
                                IconButton(onClick = { fetchForms(context) }) {
                                    Icon(Icons.Default.Refresh, contentDescription = "رفرش")
                                }
                            }
                        )
                    },
                    floatingActionButton = {
                        FloatingActionButton(onClick = {
                            lifecycleScope.launch(Dispatchers.IO) {
                                createPhoneExcel(context, formList)
                            }
                        }) {
                            Text("📄")
                        }
                    }
                ) { padding ->
                    Surface(modifier = Modifier.padding(padding)) {
                        FormListScreen(formList)
                    }
                }
            }
        }

        fetchForms(this)
    }

    private fun fetchForms(context: ComponentActivity) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val forms = api.getForms()
                formList.clear()
                formList.addAll(forms)
            } catch (e: Exception) {
                e.printStackTrace()
                launch(Dispatchers.Main) {
                    Toast.makeText(context, "خطا در دریافت داده‌ها یا اتصال به سرور", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
