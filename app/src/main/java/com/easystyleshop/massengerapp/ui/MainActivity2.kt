package com.easystyleshop.massengerapp.ui

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.easystyleshop.massengerapp.ui.screens.SendToEmailsContent
import com.easystyleshop.massengerapp.ui.theme.MessengerAppTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*
import javax.mail.*
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage

class MainActivity2 : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MessengerAppTheme {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        val context = LocalContext.current
//                        val fromEmail = "test.test.dev.1991@gmail.com"
//                        val password = "clce qwlq cjwl hmvc" // App Password

//                        val fromEmail = "inteligencofiran@gmail.com"
//                        val password = "uhio nepj taoi ebgb" // App Password

//                        val fromEmail = "ovichdev22@gmail.com"
//                        val passwordrd = "sfnx bjnq dogq yfyh" // App Password

                        val fromEmail = "devstest90@gmail.com"
//                        val password = "lvvo wzia syol xclo"
                        val password = "defg jfxq peyg dzuy"

                        // مشخصات اکانت Gmail (App Password الزامی است)
//                        val fromEmail = "devs92350@gmail.com"
//                        val password = "tduo qomc eclx feit" // ⚠️ فقط برای تست، نهایی نباید ثابت باشه

                        val coroutineScope = rememberCoroutineScope()

                        SendToEmailsContent(
                            onSend = { email, subject, content ->
                                coroutineScope.launch(Dispatchers.IO) {
                                    try {
                                        val props = Properties().apply {
                                            put("mail.smtp.auth", "true")
                                            put("mail.smtp.starttls.enable", "true")
                                            put("mail.smtp.host", "smtp.gmail.com")
                                            put("mail.smtp.port", "587")
                                        }

                                        val session = Session.getInstance(props, object : Authenticator() {
                                            override fun getPasswordAuthentication(): PasswordAuthentication {
                                                return PasswordAuthentication(fromEmail, password)
                                            }
                                        })

                                        val mimeMessage = MimeMessage(session).apply {
                                            setFrom(InternetAddress(fromEmail))
                                            setRecipients(Message.RecipientType.TO, InternetAddress.parse(email))
                                            setSubject(subject)
                                            setText(content)
                                        }

                                        Transport.send(mimeMessage)

                                        // نمایش موفقیت در UI
                                        launch(Dispatchers.Main) {
                                            Log.d("TAG", "Sent: ✅ Success")
                                            Toast.makeText(
                                                context,
                                                "✅ ایمیل به $email با موفقیت ارسال شد",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }

                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                        Log.d("TAG", "Sent: ❌ ${e.message}")
                                        // نمایش خطا در UI
                                        launch(Dispatchers.Main) {
                                            Log.d("TAG", "Sent: ${e.message}")
                                            Toast.makeText(
                                                context,
                                                "❌ خطا در ارسال به $email: ${e.message}",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }
                                    }
                                }
                            },
                            isLoading = false
                        )
                    }
                }
            }
        }
    }
}
