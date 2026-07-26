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

                                        // Register JAF DataContentHandlers for Android Compatibility
                                        try {
                                            val mc = javax.activation.CommandMap.getDefaultCommandMap() as javax.activation.MailcapCommandMap
                                            mc.addMailcap("text/html;; x-java-content-handler=com.sun.mail.handlers.text_html")
                                            mc.addMailcap("text/xml;; x-java-content-handler=com.sun.mail.handlers.text_xml")
                                            mc.addMailcap("text/plain;; x-java-content-handler=com.sun.mail.handlers.text_plain")
                                            mc.addMailcap("image/*;; x-java-content-handler=com.sun.mail.handlers.image_gif")
                                            mc.addMailcap("message/rfc822;; x-java-content-handler=com.sun.mail.handlers.message_rfc822")
                                            mc.addMailcap("multipart/*;; x-java-content-handler=com.sun.mail.handlers.multipart_mixed")
                                            javax.activation.CommandMap.setDefaultCommandMap(mc)
                                        } catch (ex: Exception) {
                                            Log.e("MainActivity2", "Failed to register JAF Mailcap Command Map: ${ex.message}", ex)
                                        }

                                        val props = Properties().apply {
                                            put("mail.smtp.host", "smtp.gmail.com")
                                            put("mail.smtp.port", "587")
                                            put("mail.smtp.auth", "true")
                                            put("mail.smtp.starttls.enable", "true")
                                            put("mail.smtp.starttls.required", "true")
                                            put("mail.smtp.ssl.enable", "false")
                                            // Robust SMTP connection and transmission timeouts
                                            put("mail.smtp.connectiontimeout", "120000") // 2 minutes
                                            put("mail.smtp.timeout", "120000")           // 2 minutes
                                            put("mail.smtp.writetimeout", "120000")      // 2 minutes
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

                                                // Image attachment with live upload progress
                                                if (!imageUri.isNullOrBlank()) {
                                                    try {
                                                        val file = File(imageUri)
                                                        if (file.exists()) {
                                                            val imagePart = MimeBodyPart()
                                                            val dataSource = ProgressDataSource(file) { percent ->
                                                                updateStatus("در حال آپلود و ضمیمه کردن تصویر (${file.name}): $percent%...")
                                                            }
                                                            imagePart.dataHandler = DataHandler(dataSource)
                                                            imagePart.fileName = file.name
                                                            multipart.addBodyPart(imagePart)
                                                            Log.d("MainActivity2", "Attached image from path with progress: $imageUri")
                                                        } else {
                                                            Log.w("MainActivity2", "Image file does not exist: $imageUri")
                                                        }
                                                    } catch (e: Exception) {
                                                        Log.e("MainActivity2", "Failed to attach image: ${e.message}", e)
                                                    }
                                                }

                                                // Video attachment with live upload progress
                                                if (!videoUri.isNullOrBlank()) {
                                                    try {
                                                        val file = File(videoUri)
                                                        if (file.exists()) {
                                                            val videoPart = MimeBodyPart()
                                                            val dataSource = ProgressDataSource(file) { percent ->
                                                                updateStatus("در حال آپلود و ضمیمه کردن ویدیو (${file.name}): $percent%...")
                                                            }
                                                            videoPart.dataHandler = DataHandler(dataSource)
                                                            videoPart.fileName = file.name
                                                            multipart.addBodyPart(videoPart)
                                                            Log.d("MainActivity2", "Attached video from path with progress: $videoUri")
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

// Progress-tracking DataSource implementation for live attachment upload updates
class ProgressDataSource(
    private val file: File,
    private val onProgress: (percent: Int) -> Unit
) : javax.activation.DataSource {
    override fun getInputStream(): java.io.InputStream {
        val fileStream = java.io.FileInputStream(file)
        val totalBytes = file.length()
        return object : java.io.InputStream() {
            private var bytesRead: Long = 0
            private var lastPercent: Int = -1

            private fun updateProgress(len: Int) {
                if (len > 0) {
                    bytesRead += len
                    val percent = if (totalBytes > 0) (bytesRead * 100 / totalBytes).toInt() else 0
                    if (percent != lastPercent) {
                        lastPercent = percent
                        onProgress(percent)
                    }
                }
            }

            override fun read(): Int {
                val b = fileStream.read()
                if (b != -1) {
                    updateProgress(1)
                }
                return b
            }

            override fun read(b: ByteArray): Int {
                val len = fileStream.read(b)
                updateProgress(len)
                return len
            }

            override fun read(b: ByteArray, off: Int, len: Int): Int {
                val readLen = fileStream.read(b, off, len)
                updateProgress(readLen)
                return readLen
            }

            override fun close() {
                fileStream.close()
            }

            override fun available(): Int = fileStream.available()
            override fun skip(n: Long): Long = fileStream.skip(n)
        }
    }

    override fun getOutputStream(): java.io.OutputStream = throw UnsupportedOperationException()
    override fun getContentType(): String = "application/octet-stream"
    override fun getName(): String = file.name
}
