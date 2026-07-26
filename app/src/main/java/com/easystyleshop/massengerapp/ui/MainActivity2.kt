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
                                    try {
                                        val props = Properties().apply {
                                            put("mail.smtp.host", "smtp.gmail.com")
                                            put("mail.smtp.port", "587")
                                            put("mail.smtp.auth", "true")
                                            put("mail.smtp.starttls.enable", "true")
                                            put("mail.smtp.starttls.required", "true")
                                            put("mail.smtp.ssl.enable", "false")
                                        }

                                        Log.d("MainActivity2", "Initiating Gmail SMTP connection properties: host=smtp.gmail.com, port=587")
                                        Log.d("MainActivity2", "Configuring Session with Authenticator for $senderEmail")

                                        val session = Session.getInstance(props, object : Authenticator() {
                                            override fun getPasswordAuthentication(): PasswordAuthentication {
                                                Log.d("MainActivity2", "Authenticator callback triggered. Providing credentials for $senderEmail")
                                                return PasswordAuthentication(senderEmail, senderPassword)
                                            }
                                        })

                                        Log.d("MainActivity2", "Preparing MIME message for recipient: $email")
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

                                        Log.d("MainActivity2", "Obtaining SMTP transport and explicitly connecting to guarantee authentication...")
                                        val transport = session.getTransport("smtp")
                                        transport.connect("smtp.gmail.com", senderEmail, senderPassword)

                                        Log.d("MainActivity2", "Sending message via transport.sendMessage()...")
                                        transport.sendMessage(mimeMessage, mimeMessage.allRecipients)

                                        Log.d("MainActivity2", "Closing transport...")
                                        transport.close()

                                        Log.d("MainActivity2", "Email successfully sent to $email")
                                        true
                                    } catch (e: Exception) {
                                        Log.e("MainActivity2", "Failed to send email to $email. Error: ${e.message}", e)
                                        val errMsg = e.message ?: ""
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
