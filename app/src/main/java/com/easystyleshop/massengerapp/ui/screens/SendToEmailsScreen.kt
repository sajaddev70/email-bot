package com.easystyleshop.massengerapp.ui.screens

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowRight
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
import java.io.File

const val DEFAULT_TEMPLATE_SUBJECT = "Support My Android App on Google Play"
const val DEFAULT_TEMPLATE_BODY = """Dear,

I hope this email finds you well.

I recently published my Android application on Google Play after spending a great deal of time designing, developing, and testing it. It has been an exciting journey, and I would be truly grateful for your support.

If you have just a few minutes, I would sincerely appreciate it if you could install the app using the link below:

https://play.google.com/store/apps/details?id=com.calecho.calanderr.app

If you enjoy using it, a rating or a short review on Google Play would mean even more. Your support helps improve the app's visibility, reach more users, and motivates me to continue improving it with new features and updates.

I've also attached a few screenshots and a short video so you can quickly see what the app offers before installing it.

Thank you very much for your time, kindness, and support. It truly means a lot to me, and I sincerely appreciate your help.

Warm regards,

Jose Campos"""

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SendToEmailsContent(
    onSend: suspend (senderEmail: String, senderPassword: String, recipientEmail: String, subject: String, content: String, imageUri: String?, videoUri: String?) -> Boolean
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Dynamically request notification permissions on Android 13+
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (!isGranted) {
                Toast.makeText(context, "جهت مشاهده وضعیت ارسال در پس‌زمینه، دسترسی نوتیفیکیشن لازم است.", Toast.LENGTH_LONG).show()
            }
        }
    )

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // Pass composable-provided onSend logic down to EmailSendingService statically
    LaunchedEffect(onSend) {
        EmailSendingService.onSendLambda = onSend
    }

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

    // Email Message States (Will be initialized from persistent template on startup)
    var subject by remember { mutableStateOf(DEFAULT_TEMPLATE_SUBJECT) }
    var content by remember { mutableStateOf(DEFAULT_TEMPLATE_BODY) }

    // Active file names
    var activeExcelName by remember { mutableStateOf("فایل اکسل پیش‌فرض (email.xlsx)") }
    var activeTemplateName by remember { mutableStateOf("قالب پیش‌فرض (Default Email Template)") }

    // Optional Attachment States
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var selectedImageName by remember { mutableStateOf("") }
    var selectedImageSizeStr by remember { mutableStateOf("") }
    var imageLocalPath by remember { mutableStateOf<String?>(null) }

    var selectedVideoUri by remember { mutableStateOf<Uri?>(null) }
    var selectedVideoName by remember { mutableStateOf("") }
    var selectedVideoSizeStr by remember { mutableStateOf("") }
    var videoLocalPath by remember { mutableStateOf<String?>(null) }

    // Launchers for picking images and videos
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri: Uri? ->
            uri?.let {
                val sizeBytes = getUriSize(context, it)
                if (sizeBytes > 5 * 1024 * 1024) {
                    Toast.makeText(context, "خطا: حجم عکس انتخابی نباید بیشتر از ۵ مگابایت باشد.", Toast.LENGTH_LONG).show()
                } else {
                    selectedImageUri = it
                    selectedImageName = getUriFileName(context, it)
                    selectedImageSizeStr = String.format(Locale.US, "%.2f MB", sizeBytes.toDouble() / (1024.0 * 1024.0))
                    imageLocalPath = copyUriToCache(context, it, "attached_image")
                }
            }
        }
    )

    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri: Uri? ->
            uri?.let {
                val sizeBytes = getUriSize(context, it)
                if (sizeBytes > 5 * 1024 * 1024) {
                    Toast.makeText(context, "خطا: حجم ویدیو انتخابی نباید بیشتر از ۵ مگابایت باشد.", Toast.LENGTH_LONG).show()
                } else {
                    selectedVideoUri = it
                    selectedVideoName = getUriFileName(context, it)
                    selectedVideoSizeStr = String.format(Locale.US, "%.2f MB", sizeBytes.toDouble() / (1024.0 * 1024.0))
                    videoLocalPath = copyUriToCache(context, it, "attached_video")
                }
            }
        }
    )

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

    // Load persistent template and active Excel details, and perform startup database checks
    LaunchedEffect(Unit) {
        scope.launch(Dispatchers.IO) {
            // 1. Load active template details
            val savedTemplateName = sharedPrefs.getString("custom_template_name", "") ?: ""
            val templateFile = File(context.filesDir, "custom_template.txt")
            if (savedTemplateName.isNotEmpty() && templateFile.exists()) {
                try {
                    val savedText = templateFile.readText()
                    val parsed = parseTemplateText(savedText)
                    if (parsed != null) {
                        subject = parsed.first
                        content = parsed.second
                        activeTemplateName = savedTemplateName
                    } else {
                        templateFile.delete()
                        sharedPrefs.edit().remove("custom_template_name").apply()
                        subject = DEFAULT_TEMPLATE_SUBJECT
                        content = DEFAULT_TEMPLATE_BODY
                        activeTemplateName = "قالب پیش‌فرض (Default Email Template)"
                    }
                } catch (e: Exception) {
                    subject = DEFAULT_TEMPLATE_SUBJECT
                    content = DEFAULT_TEMPLATE_BODY
                    activeTemplateName = "قالب پیش‌فرض (Default Email Template)"
                }
            } else {
                subject = DEFAULT_TEMPLATE_SUBJECT
                content = DEFAULT_TEMPLATE_BODY
                activeTemplateName = "قالب پیش‌فرض (Default Email Template)"
            }

            // 2. Load active Excel details
            val savedExcelUri = sharedPrefs.getString("custom_excel_uri", "") ?: ""
            val savedExcelName = sharedPrefs.getString("custom_excel_name", "") ?: ""
            if (savedExcelUri.isNotEmpty()) {
                activeExcelName = savedExcelName.ifEmpty { "فایل اکسل سفارشی" }
            } else {
                activeExcelName = "فایل اکسل پیش‌فرض (email.xlsx)"
            }

            // 3. Queue verification & self-healing load
            val allItems = emailQueueDao.getAllItems()
            val hasCorruptedData = allItems.any {
                it.email == "ROW" ||
                it.email.toDoubleOrNull() != null ||
                !android.util.Patterns.EMAIL_ADDRESS.matcher(it.email).matches()
            }
            if (allItems.isEmpty() || hasCorruptedData) {
                emailQueueDao.deleteAll()

                // Reset stats in preferences
                sharedPrefs.edit()
                    .putInt("batch_processed_count", 0)
                    .putBoolean("is_batch_paused", false)
                    .putLong("sending_start_time", 0L)
                    .apply()

                val activeEmails = loadEmailsFromActiveSource(context)
                if (activeEmails.isNotEmpty()) {
                    val queueItems = activeEmails.map { email ->
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
                try {
                    context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    scope.launch(Dispatchers.IO) {
                        val imported = readEmailsFromExcel(context, uri)
                        if (imported.isNotEmpty()) {
                            val displayName = getUriFileName(context, uri)

                            sharedPrefs.edit()
                                .putString("custom_excel_uri", uri.toString())
                                .putString("custom_excel_name", displayName)
                                .putInt("batch_processed_count", 0)
                                .putBoolean("is_batch_paused", false)
                                .putLong("sending_start_time", 0L)
                                .apply()

                            emailQueueDao.deleteAll() // Clear old queue
                            val queueItems = imported.map { email ->
                                EmailQueueItem(email = email, subject = subject, content = content)
                            }
                            emailQueueDao.insertAll(queueItems)

                            withContext(Dispatchers.Main) {
                                activeExcelName = displayName
                                Toast.makeText(context, "تعداد ${imported.size} ایمیل جدید از $displayName با موفقیت وارد دیتابیس شد.", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, "هیچ ایمیل معتبری در فایل پیدا نشد", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(context, "خطا در بارگذاری فایل اکسل: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    )

    // Template file picker launcher (.txt)
    val templateLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri: Uri? ->
            uri?.let {
                try {
                    context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    scope.launch(Dispatchers.IO) {
                        context.contentResolver.openInputStream(uri)?.use { inputStream ->
                            val text = inputStream.bufferedReader().use { it.readText() }
                            val parsed = parseTemplateText(text)
                            if (parsed != null) {
                                // Save local copy
                                val templateFile = File(context.filesDir, "custom_template.txt")
                                templateFile.writeText(text)

                                val displayName = getUriFileName(context, uri)
                                sharedPrefs.edit()
                                    .putString("custom_template_name", displayName)
                                    .apply()

                                // Update remaining pending items in database
                                val pending = emailQueueDao.getAllPending()
                                if (pending.isNotEmpty()) {
                                    val updated = pending.map {
                                        it.copy(subject = parsed.first, content = parsed.second)
                                    }
                                    emailQueueDao.insertAll(updated)
                                }

                                withContext(Dispatchers.Main) {
                                    subject = parsed.first
                                    content = parsed.second
                                    activeTemplateName = displayName
                                    Toast.makeText(context, "قالب ایمیل با موفقیت از فایل $displayName بارگذاری شد.", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(context, "خطا: فرمت فایل متنی قالب نامعتبر است.", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(context, "خطا در خواندن فایل قالب: ${e.message}", Toast.LENGTH_LONG).show()
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
                    text = "نمای بخش‌بندی شده Material 3",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
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

            // 1. SENDER SETTINGS CARD
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Settings, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Sender Settings (تنظیمات فرستنده)",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            if (isServiceRunning) {
                                Badge(
                                    containerColor = Color(0xFFE8F5E9),
                                    contentColor = Color(0xFF2E7D32)
                                ) {
                                    Text("سرویس فعال", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                }
                            } else {
                                Badge(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                ) {
                                    Text("متوقف شده", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
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
                                        contentDescription = "مشاهده کلمه عبور"
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
                            onValueChange = { delaySecondsStr = it },
                            enabled = !isServiceRunning,
                            label = { Text("فاصله زمانی ارسال بین هر ایمیل (ثانیه)") },
                            leadingIcon = { Icon(Icons.Default.Timer, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                    }
                }
            }

            // 2. EMAIL RECIPIENTS CARD
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.People, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Email Recipients (مخاطبین ایمیل)",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "منبع فعال: $activeExcelName",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("کل آدرس‌ها", style = MaterialTheme.typography.labelSmall)
                                Text("$totalCount", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
                            }
                            Column {
                                Text("ارسال موفق", style = MaterialTheme.typography.labelSmall)
                                Text("$sentCount", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Color(0xFF2E7D32))
                            }
                            Column {
                                Text("در صف انتظار", style = MaterialTheme.typography.labelSmall)
                                Text("$pendingCount", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Color(0xFFE65100))
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    excelLauncher.launch(arrayOf("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                                },
                                enabled = !isServiceRunning,
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.AttachFile, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("انتخاب اکسل جدید", fontSize = 11.sp)
                            }

                            val isCustomExcelActive = sharedPrefs.getString("custom_excel_uri", null) != null
                            Button(
                                onClick = {
                                    scope.launch(Dispatchers.IO) {
                                        sharedPrefs.edit()
                                            .remove("custom_excel_uri")
                                            .remove("custom_excel_name")
                                            .putInt("batch_processed_count", 0)
                                            .putBoolean("is_batch_paused", false)
                                            .putLong("sending_start_time", 0L)
                                            .apply()

                                        emailQueueDao.deleteAll()
                                        val defaultEmails = readEmailsFromRawResource(context)
                                        if (defaultEmails.isNotEmpty()) {
                                            val queueItems = defaultEmails.map { email ->
                                                EmailQueueItem(email = email, subject = subject, content = content)
                                            }
                                            emailQueueDao.insertAll(queueItems)
                                        }

                                        withContext(Dispatchers.Main) {
                                            activeExcelName = "فایل اکسل پیش‌فرض (email.xlsx)"
                                            Toast.makeText(context, "لیست ایمیل‌ها به حالت پیش‌فرض بازنشانی شد.", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                enabled = !isServiceRunning && isCustomExcelActive,
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("بازنشانی به پیش‌فرض", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            // 3. EMAIL TEMPLATE CARD
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Description, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Email Template (قالب متن ایمیل)",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "قالب فعال: $activeTemplateName",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = subject,
                            onValueChange = { subject = it },
                            enabled = !isServiceRunning,
                            label = { Text("موضوع ایمیل ارسالی (Subject)") },
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
                            label = { Text("متن محتوای ایمیل (Body)") },
                            isError = contentError,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp),
                            maxLines = 15
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    templateLauncher.launch(arrayOf("text/plain"))
                                },
                                enabled = !isServiceRunning,
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.AttachFile, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("بارگذاری قالب (.txt)", fontSize = 11.sp)
                            }

                            val isCustomTemplateActive = sharedPrefs.getString("custom_template_name", null) != null
                            Button(
                                onClick = {
                                    scope.launch(Dispatchers.IO) {
                                        val templateFile = File(context.filesDir, "custom_template.txt")
                                        if (templateFile.exists()) {
                                            templateFile.delete()
                                        }
                                        sharedPrefs.edit()
                                            .remove("custom_template_name")
                                            .apply()

                                        val pending = emailQueueDao.getAllPending()
                                        if (pending.isNotEmpty()) {
                                            val updated = pending.map {
                                                it.copy(subject = DEFAULT_TEMPLATE_SUBJECT, content = DEFAULT_TEMPLATE_BODY)
                                            }
                                            emailQueueDao.insertAll(updated)
                                        }

                                        withContext(Dispatchers.Main) {
                                            subject = DEFAULT_TEMPLATE_SUBJECT
                                            content = DEFAULT_TEMPLATE_BODY
                                            activeTemplateName = "قالب پیش‌فرض (Default Email Template)"
                                            Toast.makeText(context, "قالب ایمیل به حالت پیش‌فرض بازنشانی شد.", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                enabled = !isServiceRunning && isCustomTemplateActive,
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("بازنشانی به پیش‌فرض", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            // 4. ATTACHMENTS CARD
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Attachments (ضمیمه کردن عکس و فیلم)",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { imagePickerLauncher.launch("image/*") },
                                enabled = !isServiceRunning,
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("انتخاب عکس", fontSize = 11.sp)
                            }

                            Button(
                                onClick = { videoPickerLauncher.launch("video/*") },
                                enabled = !isServiceRunning,
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.VideoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("انتخاب فیلم", fontSize = 11.sp)
                            }
                        }

                        if (imageLocalPath != null || videoLocalPath != null) {
                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        imageLocalPath?.let { path ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Image, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = selectedImageName.ifEmpty { "تصویر انتخاب شده" },
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                            maxLines = 1
                                        )
                                        Text(
                                            text = "حجم: $selectedImageSizeStr",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                        )
                                    }
                                }
                                if (!isServiceRunning) {
                                    IconButton(
                                        onClick = {
                                            try {
                                                File(path).delete()
                                            } catch (e: Exception) {}
                                            selectedImageUri = null
                                            selectedImageName = ""
                                            selectedImageSizeStr = ""
                                            imageLocalPath = null
                                        }
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "حذف عکس", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }

                        if (imageLocalPath != null && videoLocalPath != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        videoLocalPath?.let { path ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.VideoLibrary, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = selectedVideoName.ifEmpty { "فیلم انتخاب شده" },
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                            maxLines = 1
                                        )
                                        Text(
                                            text = "حجم: $selectedVideoSizeStr",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                        )
                                    }
                                }
                                if (!isServiceRunning) {
                                    IconButton(
                                        onClick = {
                                            try {
                                                File(path).delete()
                                            } catch (e: Exception) {}
                                            selectedVideoUri = null
                                            selectedVideoName = ""
                                            selectedVideoSizeStr = ""
                                            videoLocalPath = null
                                        }
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "حذف ویدیو", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 5. SENDING PROGRESS CARD
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.PlayCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Sending Progress (پیشرفت ارسال بسته ۵۰۰ تایی)",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

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

            // 6. LOGS & REPORT CARD
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Assessment, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Logs / Report (گزارش‌ها و خطاهای سیستم)",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    scope.launch {
                                        generateComprehensiveEmailReport(context, db)
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("دانلود گزارش اکسل", fontSize = 11.sp)
                            }

                            Button(
                                onClick = { showWrongEmailsDialog = true },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer,
                                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("لیست خطاها ($invalidFormatCount)", fontSize = 11.sp)
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
                if (isServiceRunning) {
                    val stopIntent = Intent(context, EmailSendingService::class.java).apply {
                        action = EmailSendingService.ACTION_STOP
                    }
                    context.startService(stopIntent)
                    Toast.makeText(context, "سرویس ارسال متوقف شد", Toast.LENGTH_SHORT).show()
                } else {
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

                    sharedPrefs.edit()
                        .putString("sender_email", senderEmail)
                        .putString("sender_password", senderPassword)
                        .putInt("delay_seconds", delaySec)
                        .apply()

                    if (isBatchPaused) {
                        sharedPrefs.edit()
                            .putInt("batch_processed_count", 0)
                            .putBoolean("is_batch_paused", false)
                            .apply()
                    }

                    scope.launch(Dispatchers.IO) {
                        val pending = emailQueueDao.getAllPending()
                        if (pending.isNotEmpty()) {
                            val updated = pending.map {
                                it.copy(
                                    subject = subject,
                                    content = content,
                                    imageUri = imageLocalPath,
                                    videoUri = videoLocalPath
                                )
                            }
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

// Smart Excel Reader and Template Parser Helpers

fun findEmailColumnIndex(sheet: org.apache.poi.ss.usermodel.Sheet): Int {
    val totalRows = sheet.lastRowNum
    if (totalRows < 0) return 0

    val maxRowsToScan = minOf(totalRows, 100) // Scan up to 100 rows for accuracy
    val colScores = mutableMapOf<Int, Int>()

    for (rowNum in 0..maxRowsToScan) {
        val row = sheet.getRow(rowNum) ?: continue
        for (colNum in 0 until row.lastCellNum.toInt()) {
            val cell = row.getCell(colNum) ?: continue
            val cellValue = cell.toString().trim()
            if (android.util.Patterns.EMAIL_ADDRESS.matcher(cellValue).matches()) {
                colScores[colNum] = colScores.getOrDefault(colNum, 0) + 1
            }
        }
    }

    val bestCol = colScores.entries.maxByOrNull { it.value }
    return if (bestCol != null && bestCol.value > 0) {
        bestCol.key
    } else {
        0 // Default fallback to Column A
    }
}

fun readEmailsFromSheet(sheet: org.apache.poi.ss.usermodel.Sheet): List<String> {
    val emailColIndex = findEmailColumnIndex(sheet)
    val emails = mutableListOf<String>()

    for (rowNum in 0..sheet.lastRowNum) {
        val row = sheet.getRow(rowNum) ?: continue
        if (emailColIndex < row.lastCellNum) {
            val cell = row.getCell(emailColIndex) ?: continue
            val cellValue = cell.toString().trim()
            if (android.util.Patterns.EMAIL_ADDRESS.matcher(cellValue).matches()) {
                if (!emails.contains(cellValue)) {
                    emails.add(cellValue)
                }
            }
        }
    }
    return emails
}

fun readEmailsFromRawResource(context: Context): List<String> {
    try {
        context.resources.openRawResource(com.easystyleshop.massengerapp.R.raw.email).use { inputStream ->
            val workbook = WorkbookFactory.create(inputStream)
            val sheet = workbook.getSheetAt(0)
            val emails = readEmailsFromSheet(sheet)
            workbook.close()
            return emails
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return emptyList()
}

fun readEmailsFromCustomExcelUri(context: Context, uriString: String): List<String> {
    try {
        val uri = Uri.parse(uriString)
        return readEmailsFromExcel(context, uri)
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return emptyList()
}

fun loadEmailsFromActiveSource(context: Context): List<String> {
    val sharedPrefs = context.getSharedPreferences("sender_prefs", Context.MODE_PRIVATE)
    val customUriStr = sharedPrefs.getString("custom_excel_uri", null)
    if (!customUriStr.isNullOrBlank()) {
        try {
            val emails = readEmailsFromCustomExcelUri(context, customUriStr)
            if (emails.isNotEmpty()) {
                return emails
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    return readEmailsFromRawResource(context)
}

fun parseTemplateText(content: String): Pair<String, String>? {
    if (content.isBlank()) return null
    val lines = content.lines()
    if (lines.isEmpty()) return null

    var subject = ""
    var body = ""

    val firstLine = lines.first().trim()
    if (firstLine.startsWith("Subject:", ignoreCase = true)) {
        subject = firstLine.substring("Subject:".length).trim()
        body = lines.drop(1).joinToString("\n").trim()
    } else if (firstLine.startsWith("موضوع:", ignoreCase = true)) {
        subject = firstLine.substring("موضوع:".length).trim()
        body = lines.drop(1).joinToString("\n").trim()
    } else {
        subject = firstLine
        body = lines.drop(1).joinToString("\n").trim()
    }

    if (subject.isBlank() && body.isBlank()) return null
    return Pair(subject, body)
}

fun getUriSize(context: Context, uri: Uri): Long {
    var size: Long = 0
    try {
        if (uri.scheme == "content") {
            context.contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.SIZE), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val sizeIndex = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE)
                    if (sizeIndex != -1) {
                        size = cursor.getLong(sizeIndex)
                    }
                }
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    if (size <= 0) {
        try {
            context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { fd ->
                size = fd.length
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    return size
}

fun getUriFileName(context: Context, uri: Uri): String {
    var name = ""
    try {
        if (uri.scheme == "content") {
            context.contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1) {
                        name = cursor.getString(nameIndex)
                    }
                }
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    if (name.isEmpty()) {
        name = uri.lastPathSegment ?: "file"
    }
    return name
}

fun copyUriToCache(context: Context, uri: Uri, prefix: String): String? {
    try {
        val originalName = getUriFileName(context, uri)
        val ext = originalName.substringAfterLast('.', "")
        val filename = if (ext.isNotEmpty()) "${prefix}_temp.$ext" else "${prefix}_temp"
        val cacheFile = File(context.cacheDir, filename)
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            cacheFile.outputStream().use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        }
        return cacheFile.absolutePath
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return null
}

fun readEmailsFromExcel(context: Context, uri: Uri): List<String> {
    try {
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            val workbook = WorkbookFactory.create(inputStream)
            val sheet = workbook.getSheetAt(0)
            val emails = readEmailsFromSheet(sheet)
            workbook.close()
            return emails
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return emptyList()
}
