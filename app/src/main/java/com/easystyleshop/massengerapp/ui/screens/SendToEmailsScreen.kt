package com.easystyleshop.massengerapp.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.easystyleshop.massengerapp.data.local.AppDatabase
import com.easystyleshop.massengerapp.data.model.EmailQueueItem
import com.easystyleshop.massengerapp.data.model.SentEmailReport
import com.easystyleshop.massengerapp.util.createSentEmailsReportExcel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.poi.ss.usermodel.WorkbookFactory
import java.text.SimpleDateFormat
import java.util.*

data class LiveLog(
    val timestamp: String,
    val message: String,
    val isSuccess: Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SendToEmailsContent(
    onSend: suspend (senderEmail: String, senderPassword: String, recipientEmail: String, subject: String, content: String) -> Boolean
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Database instance
    val db = remember { AppDatabase.getDatabase(context) }
    val emailQueueDao = db.emailQueueDao()

    // Shared preferences for persistent sender configuration
    val sharedPrefs = remember { context.getSharedPreferences("sender_prefs", Context.MODE_PRIVATE) }

    // Sender UI States
    var senderEmail by remember {
        mutableStateOf(sharedPrefs.getString("sender_email", "devstest90@gmail.com") ?: "devstest90@gmail.com")
    }
    var senderPassword by remember {
        mutableStateOf(sharedPrefs.getString("sender_password", "lvvo wzia syol xclo") ?: "lvvo wzia syol xclo")
    }
    var passwordVisible by remember { mutableStateOf(false) }

    // Email Message States
    var subject by remember { mutableStateOf("موضوع پیام تستی") }
    var content by remember { mutableStateOf("سلام، این یک ایمیل تستی خودکار است.") }

    // Validation States
    var emailError by remember { mutableStateOf(false) }
    var contentError by remember { mutableStateOf(false) }

    // Queue & Send Status
    val queueList = remember { mutableStateListOf<EmailQueueItem>() }
    var totalEmailsLoaded by remember { mutableStateOf(0) }
    var isSending by remember { mutableStateOf(false) }
    var sourceName by remember { mutableStateOf("فایل اکسل پیش‌فرض (email.xlsx)") }

    // Batch Control State
    var currentBatchNum by remember { mutableStateOf(1) }
    var batchSentCount by remember { mutableStateOf(0) }
    var batchFailedCount by remember { mutableStateOf(0) }
    var isBatchPausedForCredentials by remember { mutableStateOf(false) }

    // Live Logs
    val liveLogs = remember { mutableStateListOf<LiveLog>() }
    val logsListState = rememberLazyListState()

    // Helper to add live log
    fun addLog(msg: String, isSuccess: Boolean) {
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val currentTime = sdf.format(Date())
        liveLogs.add(LiveLog(currentTime, msg, isSuccess))
    }

    // Helper to fetch latest from DB
    fun refreshQueue() {
        scope.launch(Dispatchers.IO) {
            val pending = emailQueueDao.getAllPending()
            withContext(Dispatchers.Main) {
                queueList.clear()
                queueList.addAll(pending)
                if (totalEmailsLoaded == 0) {
                    totalEmailsLoaded = pending.size
                }
            }
        }
    }

    // Load Initial Queue (either from DB or from Raw Excel)
    LaunchedEffect(Unit) {
        scope.launch(Dispatchers.IO) {
            var pendingCount = emailQueueDao.getPendingCount()
            if (pendingCount == 0) {
                // DB is empty, parse from raw email.xlsx automatically
                addLog("در حال بارگذاری خودکار ایمیل‌ها از فایل پیش‌فرض...", true)
                val rawEmails = readEmailsFromRawResource(context)
                if (rawEmails.isNotEmpty()) {
                    val queueItems = rawEmails.map { email ->
                        EmailQueueItem(email = email, subject = subject, content = content)
                    }
                    emailQueueDao.insertAll(queueItems)
                    pendingCount = emailQueueDao.getPendingCount()
                    addLog("تعداد $pendingCount ایمیل به طور خودکار بارگذاری و در دیتابیس ذخیره شد.", true)
                } else {
                    addLog("فایل پیش‌فرض ایمیل یافت نشد یا خالی است.", false)
                }
            } else {
                addLog("بازیابی $pendingCount ایمیل ارسال‌نشده از دوره قبلی در دیتابیس محلی...", true)
            }

            val pendingList = emailQueueDao.getAllPending()
            withContext(Dispatchers.Main) {
                queueList.clear()
                queueList.addAll(pendingList)
                totalEmailsLoaded = pendingList.size
            }
        }
    }

    // Excel file picker for custom file
    val excelLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri: Uri? ->
            uri?.let {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                scope.launch(Dispatchers.IO) {
                    addLog("در حال خواندن فایل اکسل انتخاب شده...", true)
                    val imported = readEmailsFromExcel(context, uri)
                    if (imported.isNotEmpty()) {
                        emailQueueDao.deleteAll() // Clear old queue
                        val queueItems = imported.map { email ->
                            EmailQueueItem(email = email, subject = subject, content = content)
                        }
                        emailQueueDao.insertAll(queueItems)
                        val pendingList = emailQueueDao.getAllPending()
                        withContext(Dispatchers.Main) {
                            queueList.clear()
                            queueList.addAll(pendingList)
                            totalEmailsLoaded = pendingList.size
                            sourceName = "فایل اکسل سفارشی"
                            currentBatchNum = 1
                            batchSentCount = 0
                            batchFailedCount = 0
                            isBatchPausedForCredentials = false
                        }
                        addLog("تعداد ${pendingList.size} ایمیل جدید با موفقیت وارد دیتابیس شد.", true)
                    } else {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "هیچ ایمیل معتبری در فایل پیدا نشد", Toast.LENGTH_LONG).show()
                        }
                        addLog("فایل انتخابی فاقد آدرس ایمیل معتبر بود.", false)
                    }
                }
            }
        }
    )

    // Auto scroll logs to bottom when new log added
    LaunchedEffect(liveLogs.size) {
        if (liveLogs.isNotEmpty()) {
            logsListState.animateScrollToItem(liveLogs.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        // TOP HEADER
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "سامانه ارسال ایمیل هوشمند",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = sourceName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
            }
            IconButton(
                onClick = {
                    excelLauncher.launch(arrayOf("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                },
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            ) {
                Icon(Icons.Default.AttachFile, contentDescription = "Import Custom Excel")
            }
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // CARD 1: QUEUE STATUS CARD
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "وضعیت صف ارسال ایمیل",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "کل ایمیل‌ها:",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                )
                                Text(
                                    text = "$totalEmailsLoaded",
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "ارسال نشده (در صف):",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                )
                                Text(
                                    text = "${queueList.size}",
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }

            // CARD 2: SENDER CONFIGURATION CARD
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "تنظیمات اکانت فرستنده ایمیل",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (isSending) {
                                Badge(
                                    containerColor = MaterialTheme.colorScheme.errorContainer,
                                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                                ) {
                                    Text("قفل شده", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                }
                            } else if (isBatchPausedForCredentials) {
                                Badge(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                ) {
                                    Text("نیاز به اکانت جدید", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                }
                            } else {
                                Badge(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                ) {
                                    Text("آماده ورود داده", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = senderEmail,
                            onValueChange = {
                                senderEmail = it
                                emailError = false
                            },
                            enabled = !isSending,
                            label = { Text("ایمیل فرستنده (جیمیل)") },
                            isError = emailError,
                            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = senderPassword,
                            onValueChange = { senderPassword = it },
                            enabled = !isSending,
                            label = { Text("کلمه عبور برنامه (App Password)") },
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = "Toggle password visibility"
                                    )
                                }
                            },
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                        )
                    }
                }
            }

            // CARD 3: MESSAGE CONTENT CARD (Collapsed style)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "موضوع و متن ایمیل ارسالی",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = subject,
                            onValueChange = { subject = it },
                            enabled = !isSending,
                            label = { Text("موضوع ایمیل") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = content,
                            onValueChange = {
                                content = it
                                contentError = false
                            },
                            enabled = !isSending,
                            label = { Text("محتوای پیام") },
                            isError = contentError,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp),
                            maxLines = 4
                        )
                    }
                }
            }

            // CARD 4: PROGRESS & CONTROL CARD
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "وضعیت پیشرفت ارسال (بسته‌های ۵۰۰ تایی)",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Current batch and counters
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "بسته فعلی: شماره $currentBatchNum",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "موفق: $batchSentCount | ناموفق: $batchFailedCount",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Progress Calculation
                        val processedInBatch = batchSentCount + batchFailedCount
                        val progressFraction = if (totalEmailsLoaded > 0) {
                            (totalEmailsLoaded - queueList.size).toFloat() / totalEmailsLoaded.toFloat()
                        } else {
                            0f
                        }
                        val percentage = (progressFraction * 100).toInt()

                        LinearProgressIndicator(
                            progress = { progressFraction },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.primaryContainer
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "پیشرفت کل: $percentage%",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                            Text(
                                text = "باقی‌مانده کل: ${queueList.size} ایمیل",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }

            // CARD 5: LIVE LOGS CARD
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "گزارش زنده ارسال (کنسول)",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = Color.LightGray,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        HorizontalDivider(color = Color.DarkGray, modifier = Modifier.padding(bottom = 6.dp))

                        if (liveLogs.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "هیچ فعالیتی ثبت نشده است",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray
                                )
                            }
                        } else {
                            LazyColumn(
                                state = logsListState,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(liveLogs) { log ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "[${log.timestamp}] ",
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                            color = Color.Cyan
                                        )
                                        Text(
                                            text = log.message,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (log.isSuccess) Color.Green else Color.Red
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // BOTTOM ACTION BUTTON
        Button(
            onClick = {
                if (isSending) return@Button

                // Validate credentials
                if (senderEmail.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(senderEmail).matches()) {
                    emailError = true
                    Toast.makeText(context, "لطفاً یک ایمیل فرستنده معتبر وارد کنید", Toast.LENGTH_SHORT).show()
                    return@Button
                }

                if (senderPassword.isBlank()) {
                    Toast.makeText(context, "لطفاً کلمه عبور فرستنده را وارد کنید", Toast.LENGTH_SHORT).show()
                    return@Button
                }

                if (content.isBlank()) {
                    contentError = true
                    Toast.makeText(context, "محتوای ایمیل نباید خالی باشد", Toast.LENGTH_SHORT).show()
                    return@Button
                }

                // Save credentials to SharedPrefs securely
                sharedPrefs.edit()
                    .putString("sender_email", senderEmail)
                    .putString("sender_password", senderPassword)
                    .apply()

                // Start sending current batch of up to 500 emails
                isSending = true
                isBatchPausedForCredentials = false

                scope.launch(Dispatchers.IO) {
                    val pendingEmails = emailQueueDao.getAllPending()
                    if (pendingEmails.isEmpty()) {
                        withContext(Dispatchers.Main) {
                            isSending = false
                            Toast.makeText(context, "هیچ ایمیلی در صف ارسال وجود ندارد", Toast.LENGTH_SHORT).show()
                        }
                        return@launch
                    }

                    // Batching configuration
                    val currentBatchList = pendingEmails.take(500)
                    withContext(Dispatchers.Main) {
                        batchSentCount = 0
                        batchFailedCount = 0
                        addLog("شروع ارسال بسته شماره $currentBatchNum شامل ${currentBatchList.size} ایمیل...", true)
                    }

                    val sentReportsList = mutableListOf<SentEmailReport>()

                    for (item in currentBatchList) {
                        if (!isSending) break // Safe cancellation

                        withContext(Dispatchers.Main) {
                            addLog("درحال ارسال ایمیل به ${item.email}...", true)
                        }

                        // Send call
                        val success = onSend(senderEmail, senderPassword, item.email, subject, content)

                        val sdfGregorian = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
                        val gregorianTime = sdfGregorian.format(Date())

                        if (success) {
                            // Delete from DB immediately on success
                            emailQueueDao.deleteById(item.id)
                            sentReportsList.add(
                                SentEmailReport(
                                    sender = senderEmail,
                                    recipient = item.email,
                                    subject = subject,
                                    body = content,
                                    sentAtGregorian = gregorianTime
                                )
                            )
                            withContext(Dispatchers.Main) {
                                batchSentCount++
                                addLog("ارسال به ${item.email} با موفقیت انجام شد ✅", true)
                            }
                        } else {
                            // Retain in DB on failure
                            withContext(Dispatchers.Main) {
                                batchFailedCount++
                                addLog("خطا در ارسال ایمیل به ${item.email} ❌", false)
                            }
                        }

                        // Refresh local Compose queue list to update UI count
                        val updatedPending = emailQueueDao.getAllPending()
                        withContext(Dispatchers.Main) {
                            queueList.clear()
                            queueList.addAll(updatedPending)
                        }

                        // Required 5 seconds delay between each email
                        delay(5_000)
                    }

                    // Save Excel Report for this batch immediately
                    if (sentReportsList.isNotEmpty()) {
                        withContext(Dispatchers.Main) {
                            createSentEmailsReportExcel(context, sentReportsList)
                        }
                    }

                    // Check if we finished the batch successfully and have more pending items
                    val remainingPendingCount = emailQueueDao.getPendingCount()

                    withContext(Dispatchers.Main) {
                        isSending = false

                        val summaryMsg = "\n=== خلاصه عملیات بسته $currentBatchNum ===\n" +
                                         "تعداد کل ارسالی موفق این بسته: $batchSentCount\n" +
                                         "تعداد ارسالی ناموفق این بسته: $batchFailedCount\n" +
                                         "کل باقی‌مانده در صف دیتابیس: $remainingPendingCount\n" +
                                         "=============================="
                        addLog(summaryMsg, true)

                        if (remainingPendingCount > 0) {
                            // Pause and ask for next credentials
                            isBatchPausedForCredentials = true
                            currentBatchNum++
                            addLog("بسته کامل شد. برنامه موقتاً متوقف شد. لطفاً اطلاعات اکانت جدید را وارد کنید و روی 'بعدی' کلیک کنید.", true)
                            Toast.makeText(context, "بسته کامل شد. لطفاً اطلاعات اکانت بعدی را وارد کنید.", Toast.LENGTH_LONG).show()
                        } else {
                            // Fully finished!
                            currentBatchNum = 1
                            addLog("ارسال تمامی ایمیل‌های موجود در صف با موفقیت به پایان رسید! 🎉", true)
                            Toast.makeText(context, "ارسال تمامی ایمیل‌ها به پایان رسید", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            },
            enabled = !isSending && queueList.isNotEmpty(),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isBatchPausedForCredentials) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            if (isSending) {
                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text("در حال ارسال ایمیل‌ها...", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            } else {
                val buttonText = if (isBatchPausedForCredentials) {
                    "بعدی (ارسال بسته $currentBatchNum)"
                } else {
                    "شروع ارسال ایمیل‌ها"
                }
                Icon(
                    imageVector = if (isBatchPausedForCredentials) Icons.Default.SkipNext else Icons.Default.PlayArrow,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(buttonText, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// Read emails helper from raw resources
fun readEmailsFromRawResource(context: Context): List<String> {
    val emails = mutableListOf<String>()
    try {
        context.resources.openRawResource(com.easystyleshop.massengerapp.R.raw.email).use { inputStream ->
            val workbook = WorkbookFactory.create(inputStream)
            val sheet = workbook.getSheetAt(0)
            for (row in sheet) {
                val cell = row.getCell(0)
                val email = cell?.toString()?.trim()
                if (!email.isNullOrBlank() && android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    emails.add(email)
                }
            }
            workbook.close()
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return emails
}

// Read emails from URI helper
fun readEmailsFromExcel(context: Context, uri: Uri): List<String> {
    val emails = mutableListOf<String>()
    try {
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            val workbook = WorkbookFactory.create(inputStream)
            val sheet = workbook.getSheetAt(0)
            for (row in sheet) {
                val cell = row.getCell(0)
                val email = cell?.toString()?.trim()
                if (!email.isNullOrBlank() && android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    emails.add(email)
                }
            }
            workbook.close()
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return emails
}
