package com.easystyleshop.massengerapp.ui.screens

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
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
import com.easystyleshop.massengerapp.service.EmailSendingService
import com.easystyleshop.massengerapp.util.generateComprehensiveEmailReport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.poi.ss.usermodel.WorkbookFactory
import java.text.SimpleDateFormat
import java.util.*

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

    // Persistent State Variables
    var senderEmail by remember {
        mutableStateOf(sharedPrefs.getString("sender_email", "test.test.dev.1991@gmail.com") ?: "test.test.dev.1991@gmail.com")
    }
    var senderPassword by remember {
        mutableStateOf(sharedPrefs.getString("sender_password", "cgfy nwwj peir orlu") ?: "cgfy nwwj peir orlu")
    }
    var delaySecondsStr by remember {
        mutableStateOf(sharedPrefs.getInt("delay_seconds", 60).toString())
    }
    var passwordVisible by remember { mutableStateOf(false) }

    // Email Message States
    var subject by remember { mutableStateOf("موضوع پیام تستی") }
    var content by remember { mutableStateOf("سلام، این یک ایمیل تستی خودکار است.") }

    // Validation States
    var emailError by remember { mutableStateOf(false) }
    var contentError by remember { mutableStateOf(false) }
    
    // Auto sync stats from Database using Flows
    val pendingCount by emailQueueDao.getPendingCountFlow().collectAsState(initial = 0)
    val sentCount by emailQueueDao.getSentCountFlow().collectAsState(initial = 0)
    val invalidFormatCount by emailQueueDao.getInvalidFormatCountFlow().collectAsState(initial = 0)
    val smtpRejectedCount by emailQueueDao.getSmtpRejectedCountFlow().collectAsState(initial = 0)
    val totalCount = pendingCount + sentCount + invalidFormatCount + smtpRejectedCount

    // Listen to background service status from SharedPreferences
    var isBatchPaused by remember { mutableStateOf(sharedPrefs.getBoolean("is_batch_paused", false)) }
    var isServiceRunning by remember { mutableStateOf(sharedPrefs.getBoolean("is_service_running", false)) }
    var batchProcessedCount by remember { mutableStateOf(sharedPrefs.getInt("batch_processed_count", 0)) }

    DisposableEffect(sharedPrefs) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { prefs, key ->
            when (key) {
                "is_batch_paused" -> isBatchPaused = prefs.getBoolean("is_batch_paused", false)
                "is_service_running" -> isServiceRunning = prefs.getBoolean("is_service_running", false)
                "batch_processed_count" -> batchProcessedCount = prefs.getInt("batch_processed_count", 0)
            }
        }
        sharedPrefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose {
            sharedPrefs.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }

    var sourceName by remember { mutableStateOf("فایل اکسل پیش‌فرض (email.xlsx)") }
    var showWrongEmailsDialog by remember { mutableStateOf(false) }
    var wrongEmailsList by remember { mutableStateOf(listOf<EmailQueueItem>()) }

    // Fetch wrong emails for display
    LaunchedEffect(invalidFormatCount, smtpRejectedCount, showWrongEmailsDialog) {
        if (showWrongEmailsDialog) {
            scope.launch(Dispatchers.IO) {
                val all = emailQueueDao.getAllItems()
                val wrong = all.filter { it.status == "INVALID_FORMAT" || it.status == "SMTP_REJECTED" }
                withContext(Dispatchers.Main) {
                    wrongEmailsList = wrong
                }
            }
        }
    }

    // Load Initial Queue from Raw Excel if DB is fully empty
    LaunchedEffect(Unit) {
        scope.launch(Dispatchers.IO) {
            val totalExisting = emailQueueDao.getAllItems().size
            if (totalExisting == 0) {
                val rawEmails = readEmailsFromRawResource(context)
                if (rawEmails.isNotEmpty()) {
                    val queueItems = rawEmails.map { email ->
                        EmailQueueItem(email = email, subject = subject, content = content)
                    }
                    emailQueueDao.insertAll(queueItems)
                }
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
                    val imported = readEmailsFromExcel(context, uri)
                    if (imported.isNotEmpty()) {
                        emailQueueDao.deleteAll() // Clear old queue

                        // Reset stats in preferences
                        sharedPrefs.edit()
                            .putInt("batch_processed_count", 0)
                            .putBoolean("is_batch_paused", false)
                            .putLong("sending_start_time", 0L)
                            .apply()

                        val queueItems = imported.map { email ->
                            EmailQueueItem(email = email, subject = subject, content = content)
                        }
                        emailQueueDao.insertAll(queueItems)

                        withContext(Dispatchers.Main) {
                            sourceName = "فایل اکسل سفارشی"
                            Toast.makeText(context, "تعداد ${imported.size} ایمیل جدید با موفقیت وارد دیتابیس شد.", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "هیچ ایمیل معتبری در فایل پیدا نشد", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }
    )

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
                    text = "سامانه ارسال ایمیل هوشمند (فعال در پس‌زمینه)",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = sourceName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(
                    onClick = {
                        scope.launch {
                            generateComprehensiveEmailReport(context, db)
                        }
                    },
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                ) {
                    Icon(Icons.Default.Download, contentDescription = "دانلود گزارش اکسل")
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
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // BATCH LIMIT / SERVICE WARNING BANNER
            if (isBatchPaused) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "بسته ۵۰۰ تایی قبلی با موفقیت به پایان رسید یا فرستنده نامعتبر است. جهت ادامه ارسال، لطفا اطلاعات فرستنده جدید را وارد کرده و دکمه 'ادامه ارسال' را بزنید.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }

            // CARD 1: QUEUE STATUS CARD
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "وضعیت صف ارسال ایمیل (همگام‌سازی خودکار)",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("کل ایمیل‌ها:", style = MaterialTheme.typography.bodySmall)
                                Text("$totalCount", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
                            }
                            Column {
                                Text("موفق:", style = MaterialTheme.typography.bodySmall)
                                Text("$sentCount", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Color(0xFF2E7D32))
                            }
                            Column {
                                Text("باقی‌مانده:", style = MaterialTheme.typography.bodySmall)
                                Text("$pendingCount", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Color(0xFFE65100))
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("فرمت اشتباه: $invalidFormatCount", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                                Text("رد شده توسط سرور (SMTP): $smtpRejectedCount", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                            }

                            if (invalidFormatCount + smtpRejectedCount > 0) {
                                TextButton(onClick = { showWrongEmailsDialog = true }) {
                                    Text("مشاهده لیست خطاکارها", style = MaterialTheme.typography.labelMedium)
                                    Icon(Icons.Default.ArrowRight, contentDescription = null)
                                }
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
                            if (isServiceRunning) {
                                Badge(
                                    containerColor = Color(0xFFE8F5E9),
                                    contentColor = Color(0xFF2E7D32)
                                ) {
                                    Text("سرویس پس‌زمینه فعال", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                }
                            } else {
                                Badge(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                ) {
                                    Text("سرویس متوقف", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
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
                            enabled = !isServiceRunning,
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
                            enabled = !isServiceRunning,
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

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = delaySecondsStr,
                            onValueChange = {
                                delaySecondsStr = it
                            },
                            enabled = !isServiceRunning,
                            label = { Text("فاصله زمانی ارسال بین هر ایمیل (ثانیه)") },
                            leadingIcon = { Icon(Icons.Default.Timer, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
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
                            enabled = !isServiceRunning,
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
                            enabled = !isServiceRunning,
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
                            text = "وضعیت پیشرفت ارسال بسته ۵۰۰ تایی",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "پیشرفت بسته جاری: $batchProcessedCount از ۵۰۰",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        val progressFraction = batchProcessedCount.toFloat() / 500f
                        val percentage = (progressFraction * 100).toInt().coerceIn(0, 100)

                        LinearProgressIndicator(
                            progress = { progressFraction.coerceIn(0f, 1f) },
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
                                text = "پیشرفت کل بسته: $percentage%",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                            Text(
                                text = "تعداد باقی‌مانده کل صف: $pendingCount ایمیل",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // BOTTOM ACTION BUTTON
        Button(
            onClick = {
                if (isServiceRunning) {
                    // Stop service
                    val stopIntent = Intent(context, EmailSendingService::class.java).apply {
                        action = EmailSendingService.ACTION_STOP
                    }
                    context.startService(stopIntent)
                    Toast.makeText(context, "سرویس ارسال متوقف شد", Toast.LENGTH_SHORT).show()
                } else {
                    // Start or resume service
                    if (senderEmail.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(senderEmail).matches()) {
                        emailError = true
                        Toast.makeText(context, "لطفاً یک ایمیل فرستنده معتبر وارد کنید", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    if (senderPassword.isBlank()) {
                        Toast.makeText(context, "لطفاً کلمه عبور فرستنده را وارد کنید", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    val delaySec = delaySecondsStr.toIntOrNull() ?: 60

                    // Save values
                    sharedPrefs.edit()
                        .putString("sender_email", senderEmail)
                        .putString("sender_password", senderPassword)
                        .putInt("delay_seconds", delaySec)
                        .apply()

                    // If batch was paused, reset it when user chooses to resume with new credentials
                    if (isBatchPaused) {
                        sharedPrefs.edit()
                            .putInt("batch_processed_count", 0)
                            .putBoolean("is_batch_paused", false)
                            .apply()
                    }

                    // Bulk update subject and content of remaining pending emails in DB
                    scope.launch(Dispatchers.IO) {
                        val pending = emailQueueDao.getAllPending()
                        if (pending.isNotEmpty()) {
                            val updated = pending.map { it.copy(subject = subject, content = content) }
                            emailQueueDao.insertAll(updated)
                        }

                        withContext(Dispatchers.Main) {
                            val startIntent = Intent(context, EmailSendingService::class.java).apply {
                                action = EmailSendingService.ACTION_START
                            }
                            context.startForegroundService(startIntent)
                            Toast.makeText(context, "سرویس ارسال پس‌زمینه راه‌اندازی شد", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            },
            enabled = pendingCount > 0 || isServiceRunning,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isServiceRunning) MaterialTheme.colorScheme.error else if (isBatchPaused) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            if (isServiceRunning) {
                Icon(Icons.Default.Pause, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("توقف ارسال ایمیل‌ها", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            } else {
                val buttonText = if (isBatchPaused) {
                    "بعدی (ادامه ارسال بسته ۵۰۰ تایی)"
                } else {
                    "شروع ارسال ایمیل‌ها در پس‌زمینه"
                }
                Icon(
                    imageVector = if (isBatchPaused) Icons.Default.SkipNext else Icons.Default.PlayArrow,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(buttonText, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }

    // Wrong emails list AlertDialog
    if (showWrongEmailsDialog) {
        AlertDialog(
            onDismissRequest = { showWrongEmailsDialog = false },
            title = { Text("لیست ایمیل‌های اشتباه و ریجکت شده") },
            text = {
                Box(modifier = Modifier.height(300.dp).fillMaxWidth()) {
                    if (wrongEmailsList.isEmpty()) {
                        Text("هیچ ایمیل خطاداری ثبت نشده است.")
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(wrongEmailsList) { item ->
                                val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                                val timeStr = if (item.sentAt != null) sdf.format(Date(item.sentAt)) else "-"
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f))
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text("آدرس: ${item.email}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text("نوع خطا: ${if (item.status == "INVALID_FORMAT") "فرمت نامعتبر" else "ریجکت سرور (SMTP)"}", fontSize = 12.sp)
                                        Text("علت: ${item.errorMessage ?: "نامشخص"}", fontSize = 11.sp, color = Color.Red)
                                        Text("زمان تلاش: $timeStr | فرستنده: ${item.senderEmail ?: "-"}", fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showWrongEmailsDialog = false }) {
                    Text("بستن")
                }
            }
        )
    }
}

// Read emails helper from raw resources (targets Column B index 1 and skips row 0 header)
fun readEmailsFromRawResource(context: Context): List<String> {
    val emails = mutableListOf<String>()
    try {
        context.resources.openRawResource(com.easystyleshop.massengerapp.R.raw.email).use { inputStream ->
            val workbook = WorkbookFactory.create(inputStream)
            val sheet = workbook.getSheetAt(0)
            for (rowNum in 1..sheet.lastRowNum) {
                val row = sheet.getRow(rowNum) ?: continue
                val cell = row.getCell(1) // Column B (index 1)
                val value = cell?.toString()?.trim()
                if (!value.isNullOrBlank()) {
                    emails.add(value)
                }
            }
            workbook.close()
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return emails
}

// Read emails from URI helper (targets Column B index 1 and skips row 0 header)
fun readEmailsFromExcel(context: Context, uri: Uri): List<String> {
    val emails = mutableListOf<String>()
    try {
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            val workbook = WorkbookFactory.create(inputStream)
            val sheet = workbook.getSheetAt(0)
            for (rowNum in 1..sheet.lastRowNum) {
                val row = sheet.getRow(rowNum) ?: continue
                val cell = row.getCell(1) // Column B (index 1)
                val value = cell?.toString()?.trim()
                if (!value.isNullOrBlank()) {
                    emails.add(value)
                }
            }
            workbook.close()
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return emails
}
