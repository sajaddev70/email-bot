package com.easystyleshop.massengerapp.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.easystyleshop.massengerapp.ui.theme.MessengerAppTheme
import org.apache.poi.ss.usermodel.WorkbookFactory
import java.io.InputStream
import java.util.*
import javax.mail.*
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage

class MainActivity3 : ComponentActivity() {

    private val emailList = mutableStateListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val excelPickerLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    result.data?.data?.let { uri ->
                        val emails = readEmailsFromExcel(uri)
                        emailList.clear()
                        emailList.addAll(emails)
                        Toast.makeText(this, "تعداد ایمیل: ${emails.size}", Toast.LENGTH_SHORT)
                            .show()
                    }
                }
            }

        setContent {
            MessengerAppTheme {
                var subject by remember { mutableStateOf("") }
                var content by remember { mutableStateOf("") }
                var isSending by remember { mutableStateOf(false) }
                var sentSuccess by remember { mutableStateOf(0) }
                var sentFailed by remember { mutableStateOf(0) }

                Surface(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("📤 ارسال ایمیل از فایل اکسل", fontSize = 20.sp)

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(onClick = {
                            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                                type =
                                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                                addCategory(Intent.CATEGORY_OPENABLE)
                            }
                            excelPickerLauncher.launch(intent)
                        }) {
                            Text("📂 انتخاب فایل اکسل")
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = subject,
                            onValueChange = { subject = it },
                            label = { Text("موضوع") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = content,
                            onValueChange = { content = it },
                            label = { Text("متن پیام") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(150.dp)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                isSending = true
                                sentSuccess = 0
                                sentFailed = 0

                                sendEmailsToUsers(
                                    fromEmail = "inteligencofiran@gmail.com",
                                    password = "uhio nepj taoi ebgb", // App Password
                                    recipients = emailList,
                                    subject = subject,
                                    message = content,
                                    onEachComplete = { _, success, _ ->
                                        if (success) sentSuccess++
                                        else sentFailed++
                                    },
                                    onDone = {
                                        isSending = false
                                        Toast.makeText(
                                            this@MainActivity3,
                                            "✅ ارسال به همه انجام شد",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                )
                            },
                            enabled = !isSending && emailList.isNotEmpty()
                        ) {
                            Text(if (isSending) "در حال ارسال..." else "ارسال ایمیل‌ها (${emailList.size})")
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text("✅ موفق: $sentSuccess    ❌ ناموفق: $sentFailed")
                    }
                }
            }
        }
    }

    private fun readEmailsFromExcel(uri: Uri): List<String> {
        val emails = mutableListOf<String>()
        try {
            val inputStream: InputStream? = contentResolver.openInputStream(uri)
            inputStream.use { stream ->
                val workbook = WorkbookFactory.create(stream)
                val sheet = workbook.getSheetAt(0)
                for (row in sheet) {
                    val cell = row.getCell(0)
                    val email = cell?.stringCellValue
                    if (!email.isNullOrBlank()) {
                        emails.add(email.trim())
                    }
                }
                workbook.close()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "⚠️ خطا در خواندن فایل اکسل", Toast.LENGTH_LONG).show()
        }
        return emails
    }

    private fun sendEmailsToUsers(
        fromEmail: String,
        password: String,
        recipients: List<String>,
        subject: String,
        message: String,
        onEachComplete: (String, Boolean, String?) -> Unit,
        onDone: () -> Unit
    ) {
        Thread {
            for (recipient in recipients) {
                try {
                    val props = Properties().apply {
                        put("mail.smtp.auth", "true")
                        put("mail.smtp.starttls.enable", "true")
                        put("mail.smtp.host", "smtp.gmail.com")
                        put("mail.smtp.port", "587")
                    }

                    val session = Session.getInstance(props, object : Authenticator() {
                        override fun getPasswordAuthentication(): PasswordAuthentication {
                            return PasswordAuthentication(fromEmail, password)
                        }
                    })

                    val mimeMessage = MimeMessage(session).apply {
                        setFrom(InternetAddress(fromEmail))
                        setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipient))
                        setSubject(subject)
                        setText(message)
                    }

                    Transport.send(mimeMessage)

                    runOnUiThread {
                        onEachComplete(recipient, true, null)
                    }

                } catch (e: Exception) {
                    e.printStackTrace()
                    runOnUiThread {
                        onEachComplete(recipient, false, e.message)
                    }
                }

                // 🕒 Delay 15 ثانیه برای امنیت Gmail
                Thread.sleep(15_000)
            }

            runOnUiThread {
                onDone()
            }
        }.start()
    }
}
