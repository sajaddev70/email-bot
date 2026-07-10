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

                    updateNotification(
                        "در حال ارسال ایمیل‌ها...",
                        "ارسال به ${item.email} (باقی‌مانده: $totalPending)"
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

                    // 2. SMTP Sending
                    var isSuccess = false
                    try {
                        isSuccess = sendEmailSmtp(senderEmail, senderPassword, item.email, item.subject, item.content)
                    } catch (e: Exception) {
                        val errMsg = e.message ?: ""
                        Log.e("EmailSendingService", "Error sending to ${item.email}: $errMsg", e)

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
                                .apply()
                            updateNotification("خطای اعتبار سنجی", "کلمه عبور یا ایمیل فرستنده رد شد. ارسال متوقف شد.")
                            stopSelf()
                            return@launch
                        } else if (isNetworkError) {
                            sharedPrefs.edit().putBoolean("is_service_running", false).apply()
                            updateNotification("خطای اتصال شبکه", "اتصال اینترنت برقرار نیست. بعداً تلاش خواهد شد.")
                            stopSelf()
                            return@launch
                        } else {
                            val updatedItem = item.copy(
                                status = "SMTP_REJECTED",
                                senderEmail = senderEmail,
                                sentAt = System.currentTimeMillis(),
                                errorMessage = errMsg
                            )
                            db.emailQueueDao().update(updatedItem)
                        }
                    }

                    if (isSuccess) {
                        val updatedItem = item.copy(
                            status = "SENT",
                            senderEmail = senderEmail,
                            sentAt = System.currentTimeMillis()
                        )
                        db.emailQueueDao().update(updatedItem)
                    }

                    sharedPrefs.edit().putInt("batch_processed_count", processedCount + 1).apply()

                    delay(delaySeconds * 1000L)
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
        sharedPrefs.edit().putBoolean("is_service_running", false).apply()
    }

    private fun sendEmailSmtp(
        senderEmail: String,
        senderPassword: String,
        recipientEmail: String,
        subject: String,
        body: String
    ): Boolean {
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
            setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipientEmail))
            setSubject(subject)
            setText(body)
        }
        mimeMessage.saveChanges()

        val transport = session.getTransport("smtp")
        transport.connect("smtp.gmail.com", senderEmail, senderPassword)
        transport.sendMessage(mimeMessage, mimeMessage.allRecipients)
        transport.close()
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

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
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
