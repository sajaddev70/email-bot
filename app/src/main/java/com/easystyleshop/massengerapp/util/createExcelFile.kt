package com.easystyleshop.massengerapp.util

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.annotation.RequiresApi
import com.easystyleshop.massengerapp.data.local.AppDatabase
import com.easystyleshop.massengerapp.data.model.Email
import com.easystyleshop.massengerapp.data.model.FormModel
import com.easystyleshop.massengerapp.data.model.TelegramUser
import com.easystyleshop.massengerapp.data.model.SentEmailReport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*

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

suspend fun generateComprehensiveEmailReport(context: Context, db: AppDatabase) {
    withContext(Dispatchers.IO) {
        try {
            val allItems = db.emailQueueDao().getAllItems()
            val senderStats = db.emailQueueDao().getSenderStats()

            val sharedPrefs = context.getSharedPreferences("sender_prefs", Context.MODE_PRIVATE)
            val startTimeLong = sharedPrefs.getLong("sending_start_time", 0L)

            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val startTimeStr = if (startTimeLong != 0L) sdf.format(Date(startTimeLong)) else "-"
            val syncTimeStr = sdf.format(Date())

            val workbook = XSSFWorkbook()

            // SHEET 1: Summary (جمع‌بندی و گزارش خلاصه جهت ارائه به کارفرما)
            val summarySheet = workbook.createSheet("جمع‌بندی و خلاصه گزارش")

            // Set main title
            val r0 = summarySheet.createRow(0)
            r0.createCell(0).setCellValue("گزارش جامع وضعیت سامانه ارسال ایمیل (تاییدیه کارفرما)")

            // Key Statistics Rows
            val totalProcessed = allItems.count { it.status != "PENDING" }
            val totalSuccess = allItems.count { it.status == "SENT" }
            val totalFailed = allItems.count { it.status == "INVALID_FORMAT" || it.status == "SMTP_REJECTED" }
            val totalInvalidFormat = allItems.count { it.status == "INVALID_FORMAT" }
            val totalSmtpRejected = allItems.count { it.status == "SMTP_REJECTED" }
            val totalRemaining = allItems.count { it.status == "PENDING" }

            val stats = listOf(
                "تعداد کل آدرس‌های وارد شده در صف" to allItems.size,
                "کل ایمیل‌های پردازش شده در این دوره" to totalProcessed,
                "تعداد ارسال موفق (ارسال شده)" to totalSuccess,
                "تعداد کل ایمیل‌های ناموفق (فیلد شده)" to totalFailed,
                "تعداد خطا به دلیل فرمت آدرس نامعتبر" to totalInvalidFormat,
                "تعداد خطا به دلیل رد توسط سرور (SMTP)" to totalSmtpRejected,
                "تعداد کل باقی‌مانده در صف (باقی‌مانده)" to totalRemaining,
                "تاریخ و زمان شروع عملیات ارسال" to startTimeStr,
                "آخرین زمان همگام‌سازی و خروجی گزارش" to syncTimeStr
            )

            stats.forEachIndexed { idx, pair ->
                val r = summarySheet.createRow(idx + 2)
                r.createCell(0).setCellValue(pair.first)
                r.createCell(1).setCellValue(pair.second.toString())
            }

            // Failure breakdown title
            val rFailTitleRow = stats.size + 4
            val rFailTitle = summarySheet.createRow(rFailTitleRow)
            rFailTitle.createCell(0).setCellValue("تحلیل دقیق دلایل عدم ارسال (خطاها/فیلد شده)")

            val rFailHeader = summarySheet.createRow(rFailTitleRow + 1)
            rFailHeader.createCell(0).setCellValue("دلیل خطا / توضیحات سیستم")
            rFailHeader.createCell(1).setCellValue("تعداد دفعات رخ‌داد")

            val failuresGrouped = allItems
                .filter { it.status == "INVALID_FORMAT" || it.status == "SMTP_REJECTED" }
                .groupBy { it.errorMessage ?: "خطای ناشناخته در زمان ارسال" }
                .map { it.key to it.value.size }

            if (failuresGrouped.isEmpty()) {
                val r = summarySheet.createRow(rFailTitleRow + 2)
                r.createCell(0).setCellValue("هیچ خطایی در این دوره ثبت نشده است. عملکرد ۱۰۰٪ موفق!")
                r.createCell(1).setCellValue(0.0)
            } else {
                failuresGrouped.forEachIndexed { idx, pair ->
                    val r = summarySheet.createRow(rFailTitleRow + 2 + idx)
                    r.createCell(0).setCellValue(pair.first)
                    r.createCell(1).setCellValue(pair.second.toDouble())
                }
            }

            // Sender statistics title
            val rSenderTitleRow = rFailTitleRow + 4 + (if (failuresGrouped.isEmpty()) 1 else failuresGrouped.size)
            val rSenderTitle = summarySheet.createRow(rSenderTitleRow)
            rSenderTitle.createCell(0).setCellValue("آمار عملکرد اکانت‌های فرستنده")

            val rSenderHeader = summarySheet.createRow(rSenderTitleRow + 1)
            rSenderHeader.createCell(0).setCellValue("اکانت فرستنده ایمیل")
            rSenderHeader.createCell(1).setCellValue("تعداد ارسال‌های موفق")

            if (senderStats.isEmpty()) {
                val r = summarySheet.createRow(rSenderTitleRow + 2)
                r.createCell(0).setCellValue("هیچ ارسالی ثبت نشده است.")
                r.createCell(1).setCellValue(0.0)
            } else {
                senderStats.forEachIndexed { idx, stat ->
                    val r = summarySheet.createRow(rSenderTitleRow + 2 + idx)
                    r.createCell(0).setCellValue(stat.senderEmail)
                    r.createCell(1).setCellValue(stat.count.toDouble())
                }
            }


            // SHEET 2: Detailed sending log (جزئیات وضعیت تک‌تک ایمیل‌ها)
            val detailSheet = workbook.createSheet("جزئیات ارسال تک‌تک ایمیل‌ها")
            val rDetailHeader = detailSheet.createRow(0)
            rDetailHeader.createCell(0).setCellValue("ردیف")
            rDetailHeader.createCell(1).setCellValue("ایمیل فرستنده")
            rDetailHeader.createCell(2).setCellValue("ایمیل گیرنده")
            rDetailHeader.createCell(3).setCellValue("موضوع ایمیل")
            rDetailHeader.createCell(4).setCellValue("متن پیام ارسال شده")
            rDetailHeader.createCell(5).setCellValue("زمان دقیق ارسال/تلاش (با ثانیه)")
            rDetailHeader.createCell(6).setCellValue("وضعیت نهایی")
            rDetailHeader.createCell(7).setCellValue("علت خطا / توضیحات تکمیلی")

            allItems.forEachIndexed { idx, item ->
                val r = detailSheet.createRow(idx + 1)
                r.createCell(0).setCellValue((idx + 1).toDouble())
                r.createCell(1).setCellValue(item.senderEmail ?: "-")
                r.createCell(2).setCellValue(item.email)
                r.createCell(3).setCellValue(item.subject)
                r.createCell(4).setCellValue(item.content)
                r.createCell(5).setCellValue(if (item.sentAt != null) sdf.format(Date(item.sentAt)) else "-")

                val statusText = when (item.status) {
                    "SENT" -> "موفق ✅"
                    "PENDING" -> "در صف ارسال ⏳"
                    "INVALID_FORMAT" -> "ناموفق ❌ (فرمت اشتباه)"
                    "SMTP_REJECTED" -> "ناموفق ❌ (ریجکت سرور)"
                    else -> "نامشخص"
                }
                r.createCell(6).setCellValue(statusText)
                r.createCell(7).setCellValue(item.errorMessage ?: "")
            }

            val filename = "comprehensive_email_report_${System.currentTimeMillis()}.xlsx"
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
                resolver.openOutputStream(uri).use { outputStream ->
                    if (outputStream != null) {
                        workbook.write(outputStream)
                        outputStream.flush()
                    }
                }
                workbook.close()

                contentValues.clear()
                contentValues.put(MediaStore.Downloads.IS_PENDING, 0)
                resolver.update(uri, contentValues, null, null)

                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "گزارش اکسل با موفقیت تولید شد ✅", Toast.LENGTH_LONG).show()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "خطا در تولید گزارش اکسل: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}
