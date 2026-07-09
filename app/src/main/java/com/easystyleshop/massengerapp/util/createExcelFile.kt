package com.easystyleshop.massengerapp.util
import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.annotation.RequiresApi
import com.easystyleshop.massengerapp.data.model.Email
import com.easystyleshop.massengerapp.data.model.FormModel
import com.easystyleshop.massengerapp.data.model.TelegramUser
import com.easystyleshop.massengerapp.data.model.SentEmailReport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.OutputStream

fun createExcelFile(context: Context, emails: List<Email>) {
    val workbook = XSSFWorkbook()
    val sheet = workbook.createSheet("Emails")

    val header = sheet.createRow(0)
    header.createCell(0).setCellValue("From")
    header.createCell(1).setCellValue("To")
    header.createCell(2).setCellValue("Body")

    emails.forEachIndexed { index, email ->
        val row = sheet.createRow(index + 1)
        row.createCell(0).setCellValue(email.from)
        row.createCell(1).setCellValue(email.to)
        row.createCell(2).setCellValue(email.body)
    }

    val filename = "emails.xlsx"
    val mimeType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"

    val contentValues = ContentValues().apply {
        put(MediaStore.Downloads.DISPLAY_NAME, filename)
        put(MediaStore.Downloads.MIME_TYPE, mimeType)
        put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
        put(MediaStore.Downloads.IS_PENDING, 1)
    }

    val resolver = context.contentResolver
    val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)

    if (uri != null) {
        try {
            resolver.openOutputStream(uri).use { outputStream ->
                workbook.write(outputStream)
                outputStream?.flush()
            }
            workbook.close()

            contentValues.clear()
            contentValues.put(MediaStore.Downloads.IS_PENDING, 0)
            resolver.update(uri, contentValues, null, null)

            Toast.makeText(context, "File saved to Downloads!", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Error saving file: ${e.message}", Toast.LENGTH_LONG).show()
        }
    } else {
        Toast.makeText(context, "Failed to create file", Toast.LENGTH_SHORT).show()
    }
}

suspend fun createPhoneExcel(context: Context, forms: List<FormModel>) {
    withContext(Dispatchers.IO) {
        val workbook = XSSFWorkbook()
        val sheet = workbook.createSheet("Forms")

        // ایجاد هدر برای همه فیلدها
        val header = sheet.createRow(0)
        header.createCell(0).setCellValue("ID")
        header.createCell(1).setCellValue("Telegram ID")
        header.createCell(2).setCellValue("Name")
        header.createCell(3).setCellValue("First Name")
        header.createCell(4).setCellValue("Last Name")
        header.createCell(5).setCellValue("Username")
        header.createCell(6).setCellValue("Language Code")
        header.createCell(7).setCellValue("Phone")
        header.createCell(8).setCellValue("Age")
        header.createCell(9).setCellValue("Location")
        header.createCell(10).setCellValue("Job")
        header.createCell(11).setCellValue("Skills")
        header.createCell(12).setCellValue("date")

        // افزودن داده‌ها
        forms.forEachIndexed { index, form ->
            val row = sheet.createRow(index + 1)
            row.createCell(0).setCellValue(form.id.toDouble())
            row.createCell(1).setCellValue(form.telegramId.toDouble())
            row.createCell(2).setCellValue(form.name)
            row.createCell(3).setCellValue(form.firstName ?: "")
            row.createCell(4).setCellValue(form.lastName ?: "")
            row.createCell(5).setCellValue(form.userName ?: "")
            row.createCell(6).setCellValue(form.languageCode ?: "")
            row.createCell(7).setCellValue(form.phone)
            row.createCell(8).setCellValue(form.age)
            row.createCell(9).setCellValue(form.location)
            row.createCell(10).setCellValue(form.job)
            row.createCell(11).setCellValue(form.skills)
            row.createCell(12).setCellValue(form.persianDate)
        }

        val filename = "forms_${System.currentTimeMillis()}.xlsx"
        val mimeType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }

        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)

        if (uri != null) {
            try {
                resolver.openOutputStream(uri).use { outputStream ->
                    if (outputStream != null) {
                        workbook.write(outputStream)
                        outputStream.flush()
                    }
                }
                workbook.close()

                contentValues.clear()
                contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(uri, contentValues, null, null)

                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "فایل با موفقیت ذخیره شد ✅", Toast.LENGTH_LONG).show()
                }

            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "خطا در ذخیره فایل: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        } else {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "ایجاد فایل ممکن نبود ❌", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

suspend fun createTelegramUserExcel(context: Context, users: List<TelegramUser>) {
    withContext(Dispatchers.IO) {
        val workbook = XSSFWorkbook()
        val sheet = workbook.createSheet("Telegram Users")

        // هدر اکسل
        val headers = listOf("User ID", "Username", "First Name", "Last Name", "Language Code", "Start Date")
        val headerRow = sheet.createRow(0)
        headers.forEachIndexed { index, title ->
            headerRow.createCell(index).setCellValue(title)
        }

        // داده‌ها
        users.forEachIndexed { i, user ->
            val row = sheet.createRow(i + 1)
            row.createCell(0).setCellValue(user.userId.toDouble())
            row.createCell(1).setCellValue(user.username ?: "")
            row.createCell(2).setCellValue(user.firstName ?: "")
            row.createCell(3).setCellValue(user.lastName ?: "")
            row.createCell(4).setCellValue(user.languageCode ?: "")
            row.createCell(5).setCellValue(user.persianDate ?: "")
        }

        val filename = "telegram_users_${System.currentTimeMillis()}.xlsx"
        val mimeType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }

        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)

        if (uri != null) {
            try {
                resolver.openOutputStream(uri).use { outputStream ->
                    if (outputStream != null) {
                        workbook.write(outputStream)
                        outputStream.flush()
                    }
                }
                workbook.close()

                contentValues.clear()
                contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(uri, contentValues, null, null)

                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "فایل با موفقیت ذخیره شد ✅", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "خطا در ذخیره فایل: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        } else {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "امکان ایجاد فایل وجود ندارد ❌", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

fun createSentEmailsReportExcel(context: Context, reports: List<SentEmailReport>) {
    val workbook = XSSFWorkbook()
    val sheet = workbook.createSheet("Sent Emails Report")

    val header = sheet.createRow(0)
    header.createCell(0).setCellValue("Sender (از فرستنده)")
    header.createCell(1).setCellValue("Recipient (به گیرنده)")
    header.createCell(2).setCellValue("Subject (موضوع)")
    header.createCell(3).setCellValue("Body Content (محتوا)")
    header.createCell(4).setCellValue("Sent At (Gregorian میلادی)")

    reports.forEachIndexed { index, report ->
        val row = sheet.createRow(index + 1)
        row.createCell(0).setCellValue(report.sender)
        row.createCell(1).setCellValue(report.recipient)
        row.createCell(2).setCellValue(report.subject)
        row.createCell(3).setCellValue(report.body)
        row.createCell(4).setCellValue(report.sentAtGregorian)
    }

    val filename = "sent_emails_report_${System.currentTimeMillis()}.xlsx"
    val mimeType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"

    val contentValues = ContentValues().apply {
        put(MediaStore.Downloads.DISPLAY_NAME, filename)
        put(MediaStore.Downloads.MIME_TYPE, mimeType)
        put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
        put(MediaStore.Downloads.IS_PENDING, 1)
    }

    val resolver = context.contentResolver
    val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)

    if (uri != null) {
        try {
            resolver.openOutputStream(uri).use { outputStream ->
                workbook.write(outputStream)
                outputStream?.flush()
            }
            workbook.close()

            contentValues.clear()
            contentValues.put(MediaStore.Downloads.IS_PENDING, 0)
            resolver.update(uri, contentValues, null, null)

            Toast.makeText(context, "گزارش اکسل در پوشه Downloads ذخیره شد! ✅", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(context, "خطا در ذخیره گزارش اکسل: ${e.message}", Toast.LENGTH_LONG).show()
        }
    } else {
        Toast.makeText(context, "ایجاد فایل گزارش ممکن نبود ❌", Toast.LENGTH_SHORT).show()
    }
}
