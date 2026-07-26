package com.easystyleshop.massengerapp.ui

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.easystyleshop.massengerapp.ui.screens.SendToEmailsContent
import com.easystyleshop.massengerapp.ui.theme.MessengerAppTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*
import javax.mail.*
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage
import javax.mail.internet.MimeMultipart
import javax.mail.internet.MimeBodyPart
import javax.activation.FileDataSource
import javax.activation.DataHandler
import java.io.File

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
                        SendToEmailsContent(
                            onSend = { senderEmail, senderPassword, email, subject, content, imageUri, videoUri ->
                                withContext(Dispatchers.IO) {
                                    val sharedPrefs = getSharedPreferences("sender_prefs", MODE_PRIVATE)
                                    val updateStatus = { msg: String ->
                                        sharedPrefs.edit().putString("service_status_message", msg).apply()
                                        Log.d("MainActivity2", msg)
                                    }

                                    try {
                                        updateStatus("آماده‌سازی اطلاعات و اتصال برای ارسال به: $email...")

                                        val props = Properties().apply {
                                            put("mail.smtp.host", "smtp.gmail.com")
                                            put("mail.smtp.port", "587")
                                            put("mail.smtp.auth", "true")
                                            put("mail.smtp.starttls.enable", "true")
                                            put("mail.smtp.starttls.required", "true")
                                            put("mail.smtp.ssl.enable", "false")
                                        }

                                        val session = Session.getInstance(props, object : Authenticator() {
                                            override fun getPasswordAuthentication(): PasswordAuthentication {
                                                return PasswordAuthentication(senderEmail, senderPassword)
                                            }
                                        })

                                        val mimeMessage = MimeMessage(session).apply {
                                            setFrom(InternetAddress(senderEmail))
                                            setRecipients(Message.RecipientType.TO, InternetAddress.parse(email))
                                            setSubject(subject)

                                            if (imageUri.isNullOrBlank() && videoUri.isNullOrBlank()) {
                                                setText(content)
                                            } else {
                                                val multipart = MimeMultipart()

                                                // Text part
                                                val textBodyPart = MimeBodyPart().apply {
                                                    setText(content, "UTF-8")
                                                }
                                                multipart.addBodyPart(textBodyPart)

                                                // Image attachment
                                                if (!imageUri.isNullOrBlank()) {
                                                    try {
                                                        val file = File(imageUri)
                                                        if (file.exists()) {
                                                            updateStatus("در حال ضمیمه کردن و آپلود تصویر (${file.name})...")
                                                            val imagePart = MimeBodyPart()
                                                            val dataSource = FileDataSource(file)
                                                            imagePart.dataHandler = DataHandler(dataSource)
                                                            imagePart.fileName = file.name
                                                            multipart.addBodyPart(imagePart)
                                                            Log.d("MainActivity2", "Attached image from path: $imageUri")
                                                        } else {
                                                            Log.w("MainActivity2", "Image file does not exist: $imageUri")
                                                        }
                                                    } catch (e: Exception) {
                                                        Log.e("MainActivity2", "Failed to attach image: ${e.message}", e)
                                                    }
                                                }

                                                // Video attachment
                                                if (!videoUri.isNullOrBlank()) {
                                                    try {
                                                        val file = File(videoUri)
                                                        if (file.exists()) {
                                                            updateStatus("در حال ضمیمه کردن و آپلود ویدیو (${file.name})...")
                                                            val videoPart = MimeBodyPart()
                                                            val dataSource = FileDataSource(file)
                                                            videoPart.dataHandler = DataHandler(dataSource)
                                                            videoPart.fileName = file.name
                                                            multipart.addBodyPart(videoPart)
                                                            Log.d("MainActivity2", "Attached video from path: $videoUri")
                                                        } else {
                                                            Log.w("MainActivity2", "Video file does not exist: $videoUri")
                                                        }
                                                    } catch (e: Exception) {
                                                        Log.e("MainActivity2", "Failed to attach video: ${e.message}", e)
                                                    }
                                                }

                                                setContent(multipart)
                                            }
                                        }
                                        mimeMessage.saveChanges()

                                        updateStatus("در حال اتصال به سرور SMTP جیمیل...")
                                        val transport = session.getTransport("smtp")
                                        transport.connect("smtp.gmail.com", senderEmail, senderPassword)

                                        updateStatus("اتصال برقرار شد. در حال ارسال ایمیل به: $email...")
                                        transport.sendMessage(mimeMessage, mimeMessage.allRecipients)

                                        updateStatus("در حال بستن اتصال SMTP...")
                                        transport.close()

                                        updateStatus("ایمیل با موفقیت به $email ارسال شد! ✅")
                                        true
                                    } catch (e: Exception) {
                                        Log.e("MainActivity2", "Failed to send email to $email. Error: ${e.message}", e)
                                        val errMsg = e.message ?: ""
                                        updateStatus("خطا در ارسال به $email: $errMsg ❌")
                                        if (errMsg.contains("534-5.7.9") || errMsg.contains("WebLoginRequired")) {
                                            throw AuthenticationFailedException("WebLoginRequired")
                                        } else {
                                            throw e
                                        }
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
