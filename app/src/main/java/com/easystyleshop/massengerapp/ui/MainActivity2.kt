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
                                        // 1. Try secure SMTP via SSL on Port 465 (Preferred for Gmail)
                                        val props = Properties().apply {
                                            put("mail.smtp.host", "smtp.gmail.com")
                                            put("mail.smtp.port", "465")
                                            put("mail.smtp.auth", "true")
                                            put("mail.smtp.ssl.enable", "true")
                                            put("mail.smtp.socketFactory.port", "465")
                                            put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory")
                                            put("mail.smtp.socketFactory.fallback", "false")
                                            put("mail.smtp.ssl.trust", "smtp.gmail.com")
                                            put("mail.smtp.ssl.protocols", "TLSv1.2 TLSv1.3")
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
                                        mimeMessage.saveChanges()

                                        val transport = session.getTransport("smtp")
                                        transport.connect("smtp.gmail.com", senderEmail, senderPassword)
                                        transport.sendMessage(mimeMessage, mimeMessage.allRecipients)
                                        transport.close()
                                        Log.d("MainActivity2", "Email sent successfully via SSL (465) to: $email")
                                        true
                                    } catch (sslEx: Exception) {
                                        sslEx.printStackTrace()
                                        Log.e("MainActivity2", "SSL (465) failed, attempting STARTTLS (587) fallback... Error: ${sslEx.message}")

                                        try {
                                            // 2. Fallback to STARTTLS on Port 587
                                            val props = Properties().apply {
                                                put("mail.smtp.host", "smtp.gmail.com")
                                                put("mail.smtp.port", "587")
                                                put("mail.smtp.auth", "true")
                                                put("mail.smtp.starttls.enable", "true")
                                                put("mail.smtp.starttls.required", "true")
                                                put("mail.smtp.ssl.trust", "smtp.gmail.com")
                                                put("mail.smtp.ssl.protocols", "TLSv1.2 TLSv1.3")
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
                                            mimeMessage.saveChanges()

                                            val transport = session.getTransport("smtp")
                                            transport.connect("smtp.gmail.com", senderEmail, senderPassword)
                                            transport.sendMessage(mimeMessage, mimeMessage.allRecipients)
                                            transport.close()
                                            Log.d("MainActivity2", "Email sent successfully via STARTTLS (587) to: $email")
                                            true
                                        } catch (fallbackEx: Exception) {
                                            fallbackEx.printStackTrace()
                                            Log.e("MainActivity2", "Both SMTP ports (465 & 587) failed to send email to: $email. Error: ${fallbackEx.message}")

                                            // Check if it's the specific WebLoginRequired error
                                            val errMsg = fallbackEx.message ?: ""
                                            if (errMsg.contains("534-5.7.9") || errMsg.contains("WebLoginRequired")) {
                                                throw AuthenticationFailedException("WebLoginRequired")
                                            } else {
                                                throw fallbackEx
                                            }
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
