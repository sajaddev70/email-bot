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
                            onSend = { senderEmail, senderPassword, email, subject, content ->
                                withContext(Dispatchers.IO) {
                                    try {
                                        val props = Properties().apply {
                                            put("mail.smtp.auth", "true")
                                            put("mail.smtp.starttls.enable", "true")
                                            put("mail.smtp.host", "smtp.gmail.com")
                                            put("mail.smtp.port", "587")
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
                                            setText(content)
                                        }

                                        Transport.send(mimeMessage)
                                        Log.d("MainActivity2", "Email sent successfully to: $email")
                                        true
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                        Log.e("MainActivity2", "Failed to send email to: $email, error: ${e.message}")
                                        false
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
