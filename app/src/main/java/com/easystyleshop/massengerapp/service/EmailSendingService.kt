package com.easystyleshop.massengerapp.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.easystyleshop.massengerapp.data.local.AppDatabase
import com.easystyleshop.massengerapp.data.model.EmailQueueItem
import com.easystyleshop.massengerapp.ui.MainActivity2
import com.easystyleshop.massengerapp.util.generateComprehensiveEmailReport
import kotlinx.coroutines.*
import java.util.Properties
import javax.mail.Session
import javax.mail.Transport
import javax.mail.Authenticator
import javax.mail.PasswordAuthentication
import javax.mail.Message
import javax.mail.AuthenticationFailedException
import javax.mail.internet.MimeMessage
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMultipart
import javax.mail.internet.MimeBodyPart
import javax.activation.FileDataSource
import javax.activation.DataHandler
import java.io.File

class EmailSendingService : Service() {

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)
    private var sendJob: Job? = null

    private lateinit var db: AppDatabase
    private val NOTIFICATION_ID = 1001
    private val CHANNEL_ID = "email_sending_channel"

    companion object {
        const val ACTION_START = "com.easystyleshop.massengerapp.action.START"
        const val ACTION_STOP = "com.easystyleshop.massengerapp.action.STOP"

        // Static lambda reference to invoke the composable-provided onSend logic
        var onSendLambda: (suspend (senderEmail: String, senderPassword: String, recipientEmail: String, subject: String, content: String, imageUri: String?, videoUri: String?) -> Boolean)? = null
    }

    override fun onCreate() {
        super.onCreate()
        db = AppDatabase.getDatabase(applicationContext)
        createNotificationChannel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START
        if (action == ACTION_STOP) {
            stopSending()
            stopSelf()
        } else {
            startSending()
        }
        return START_STICKY
    }

    private fun startSending() {
        if (sendJob?.isActive == true) return

        val sharedPrefs = getSharedPreferences("sender_prefs", Context.MODE_PRIVATE)
        val startTime = sharedPrefs.getLong("sending_start_time", 0L)
        if (startTime == 0L) {
            sharedPrefs.edit().putLong("sending_start_time", System.currentTimeMillis()).apply()
        }

        sharedPrefs.edit()
            .putBoolean("is_batch_paused", false)
            .putBoolean("is_service_running", true)
            .apply()

        val notification = buildNotification("در حال آماده‌سازی...", "بررسی صف ارسال...")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        sendJob = serviceScope.launch {
            try {
                while (isActive) {
                    val senderEmail = sharedPrefs.getString("sender_email", "") ?: ""
                    val senderPassword = sharedPrefs.getString("sender_password", "") ?: ""
                    val delaySeconds = sharedPrefs.getInt("delay_seconds", 60)

                    if (senderEmail.isBlank() || senderPassword.isBlank()) {
                        updateNotification("خطا در تنظیمات", "ایمیل یا کلمه عبور فرستنده وارد نشده است.")
                        stopSelf()
                        return@launch
                    }

                    val pendingList = db.emailQueueDao().getAllPending()
                    if (pendingList.isEmpty()) {
                        sharedPrefs.edit()
                            .putInt("batch_processed_count", 0)
                            .putBoolean("is_batch_paused", false)
                            .putBoolean("is_service_running", false)
                            .apply()

                        generateComprehensiveEmailReport(applicationContext, db)

                        updateNotification("ارسال به پایان رسید", "تمامی ایمیل‌های موجود در صف با موفقیت پردازش شدند! 🎉")
                        stopSelf()
                        return@launch
                    }

                    val processedCount = sharedPrefs.getInt("batch_processed_count", 0)
                    if (processedCount >= 500) {
                        sharedPrefs.edit()
                            .putBoolean("is_batch_paused", true)
                            .putBoolean("is_service_running", false)
                            .apply()

                        generateComprehensiveEmailReport(applicationContext, db)

                        updateNotification(
                            "بسته ۵۰۰ تایی کامل شد ⚠️",
                            "برای ادامه ارسال، لطفاً اکانت جدید را وارد کنید."
                        )
                        stopSelf()
                        return@launch
                    }

                    val item = pendingList.first()
                    val totalPending = pendingList.size
                    val totalSent = db.emailQueueDao().getSentCount()
                    val totalInvalid = db.emailQueueDao().getInvalidFormatCount()
                    val totalRejected = db.emailQueueDao().getSmtpRejectedCount()
                    val totalFailed = totalInvalid + totalRejected

                    updateNotification(
                        "در حال ارسال ایمیل‌ها... 📧",
                        "موفق: $totalSent | خطا: $totalFailed | باقی‌مانده: $totalPending"
                    )

                    // 1. Structural Validation
                    val isValidFormat = android.util.Patterns.EMAIL_ADDRESS.matcher(item.email).matches()
                    if (!isValidFormat) {
                        val updatedItem = item.copy(
                            status = "INVALID_FORMAT",
                            senderEmail = senderEmail,
                            sentAt = System.currentTimeMillis(),
                            errorMessage = "فرمت آدرس ایمیل نامعتبر است"
                        )
                        db.emailQueueDao().update(updatedItem)
                        sharedPrefs.edit().putInt("batch_processed_count", processedCount + 1).apply()
                        continue
                    }

                    // 2. SMTP Sending (Using composable-provided onSend lambda or local SMTP fallback)
                    var isSuccess = false
                    var errorMsg: String? = null
                    try {
                        val senderLambda = onSendLambda
                        if (senderLambda != null) {
                            isSuccess = senderLambda(senderEmail, senderPassword, item.email, item.subject, item.content, item.imageUri, item.videoUri)
                        } else {
                            isSuccess = sendEmailSmtp(sharedPrefs, senderEmail, senderPassword, item.email, item.subject, item.content, item.imageUri, item.videoUri)
                        }
                    } catch (e: Exception) {
                        val errMsg = e.message ?: ""
                        Log.e("EmailSendingService", "Error sending to ${item.email}: $errMsg", e)
                        errorMsg = errMsg

                        val isAuthError = e is AuthenticationFailedException ||
                                errMsg.contains("534-5.7.9") ||
                                errMsg.contains("WebLoginRequired") ||
                                errMsg.contains("username and password not accepted") ||
                                errMsg.contains("Username and Password not accepted")

                        val isNetworkError = e is java.net.ConnectException ||
                                e is java.net.UnknownHostException ||
                                e is java.net.SocketTimeoutException ||
                                errMsg.contains("Could not connect to SMTP host") ||
                                errMsg.contains("connect timed out")

                        if (isAuthError) {
                            sharedPrefs.edit()
                                .putBoolean("is_batch_paused", true)
                                .putBoolean("is_service_running", false)
                                .putString("service_status_message", "خطای اعتبارسنجی: ایمیل یا کلمه عبور نادرست است ❌")
                                .apply()
                            updateNotification("خطای اعتبار سنجی", "کلمه عبور یا ایمیل فرستنده رد شد. ارسال متوقف شد.")
                            stopSelf()
                            return@launch
                        } else if (isNetworkError) {
                            sharedPrefs.edit()
                                .putBoolean("is_service_running", false)
                                .putString("service_status_message", "خطای اتصال شبکه: اینترنت در دسترس نیست ❌")
                                .apply()
                            updateNotification("خطای اتصال شبکه", "اتصال اینترنت برقرار نیست. بعداً تلاش خواهد شد.")
                            stopSelf()
                            return@launch
                        }
                    }

                    if (isSuccess) {
                        val updatedItem = item.copy(
                            status = "SENT",
                            senderEmail = senderEmail,
                            sentAt = System.currentTimeMillis()
                        )
                        db.emailQueueDao().update(updatedItem)
                    } else {
                        val updatedItem = item.copy(
                            status = "SMTP_REJECTED",
                            senderEmail = senderEmail,
                            sentAt = System.currentTimeMillis(),
                            errorMessage = errorMsg ?: "ارسال ناموفق یا ریجکت شده توسط سرور"
                        )
                        db.emailQueueDao().update(updatedItem)
                    }

                    sharedPrefs.edit().putInt("batch_processed_count", processedCount + 1).apply()

                    // Countdown delay with real-time status update
                    for (sec in delaySeconds downTo 1) {
                        sharedPrefs.edit()
                            .putString("service_status_message", "در حال انتظار برای ارسال ایمیل بعدی: $sec ثانیه باقی‌مانده... ⏳")
                            .apply()
                        delay(1000L)
                    }
                }
            } catch (e: CancellationException) {
                Log.d("EmailSendingService", "Sending coroutine cancelled.")
            } catch (e: Exception) {
                Log.e("EmailSendingService", "Unexpected service exception", e)
            } finally {
                sharedPrefs.edit().putBoolean("is_service_running", false).apply()
            }
        }
    }

    private fun stopSending() {
        sendJob?.cancel()
        val sharedPrefs = getSharedPreferences("sender_prefs", Context.MODE_PRIVATE)
        sharedPrefs.edit()
            .putBoolean("is_service_running", false)
            .putString("service_status_message", "سرویس ارسال توسط کاربر متوقف شد 🛑")
            .apply()
    }

    private fun sendEmailSmtp(
        sharedPrefs: android.content.SharedPreferences,
        senderEmail: String,
        senderPassword: String,
        recipientEmail: String,
        subject: String,
        body: String,
        imageUri: String?,
        videoUri: String?
    ): Boolean {
        val updateStatus = { msg: String ->
            sharedPrefs.edit().putString("service_status_message", msg).apply()
            Log.d("EmailSendingService", msg)
        }

        updateStatus("آماده‌سازی اطلاعات برای ارسال به: $recipientEmail...")

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
            setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipientEmail))
            setSubject(subject)

            if (imageUri.isNullOrBlank() && videoUri.isNullOrBlank()) {
                setText(body)
            } else {
                val multipart = MimeMultipart()

                // Text part
                val textBodyPart = MimeBodyPart().apply {
                    setText(body, "UTF-8")
                }
                multipart.addBodyPart(textBodyPart)

                // Image attachment with live upload progress tracking
                if (!imageUri.isNullOrBlank()) {
                    try {
                        val file = File(imageUri)
                        if (file.exists()) {
                            val imagePart = MimeBodyPart()
                            val dataSource = ServiceProgressDataSource(file) { percent ->
                                updateStatus("در حال آپلود و ضمیمه کردن تصویر (${file.name}): $percent%...")
                            }
                            imagePart.dataHandler = DataHandler(dataSource)
                            imagePart.fileName = file.name
                            multipart.addBodyPart(imagePart)
                            Log.d("EmailSendingService", "Attached image with progress from path: $imageUri")
                        } else {
                            Log.w("EmailSendingService", "Image file does not exist: $imageUri")
                        }
                    } catch (e: Exception) {
                        Log.e("EmailSendingService", "Failed to attach image: ${e.message}", e)
                    }
                }

                // Video attachment with live upload progress tracking
                if (!videoUri.isNullOrBlank()) {
                    try {
                        val file = File(videoUri)
                        if (file.exists()) {
                            val videoPart = MimeBodyPart()
                            val dataSource = ServiceProgressDataSource(file) { percent ->
                                updateStatus("در حال آپلود و ضمیمه کردن ویدیو (${file.name}): $percent%...")
                            }
                            videoPart.dataHandler = DataHandler(dataSource)
                            videoPart.fileName = file.name
                            multipart.addBodyPart(videoPart)
                            Log.d("EmailSendingService", "Attached video with progress from path: $videoUri")
                        } else {
                            Log.w("EmailSendingService", "Video file does not exist: $videoUri")
                        }
                    } catch (e: Exception) {
                        Log.e("EmailSendingService", "Failed to attach video: ${e.message}", e)
                    }
                }

                setContent(multipart)
            }
        }
        mimeMessage.saveChanges()

        updateStatus("در حال اتصال به SMTP جیمیل...")
        val transport = session.getTransport("smtp")
        transport.connect("smtp.gmail.com", senderEmail, senderPassword)

        updateStatus("اتصال برقرار شد. در حال ارسال ایمیل به: $recipientEmail...")
        transport.sendMessage(mimeMessage, mimeMessage.allRecipients)

        updateStatus("در حال بستن اتصال SMTP...")
        transport.close()

        updateStatus("ایمیل با موفقیت به $recipientEmail ارسال شد! ✅")
        return true
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "سامانه ارسال ایمیل پس‌زمینه",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "نمایش وضعیت ارسال ایمیل‌ها در پس‌زمینه"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(title: String, text: String): Notification {
        val stopIntent = Intent(this, EmailSendingService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            0,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Launch MainActivity2 on notification click
        val launchIntent = Intent(this, MainActivity2::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val launchPendingIntent = PendingIntent.getActivity(
            this,
            1,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .setContentIntent(launchPendingIntent) // Open app when clicked
            .addAction(android.R.drawable.ic_media_pause, "توقف ارسال", stopPendingIntent)
            .build()
    }

    private fun updateNotification(title: String, text: String) {
        val notification = buildNotification(title, text)
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, notification)
    }

    override fun onDestroy() {
        stopSending()
        serviceJob.cancel()
        super.onDestroy()
    }
}

// Progress-tracking DataSource implementation for background service fallback sends
class ServiceProgressDataSource(
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
