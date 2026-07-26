package com.easystyleshop.massengerapp

import org.junit.Test
import org.apache.poi.ss.usermodel.WorkbookFactory
import java.io.File

class ExampleUnitTest {
    @Test
    fun testAttachmentMimeMessage() {
        val props = java.util.Properties().apply {
            put("mail.smtp.host", "smtp.gmail.com")
            put("mail.smtp.port", "587")
        }
        val session = javax.mail.Session.getInstance(props)
        val message = javax.mail.internet.MimeMessage(session).apply {
            setFrom(javax.mail.internet.InternetAddress("test@gmail.com"))
            setRecipients(javax.mail.Message.RecipientType.TO, javax.mail.internet.InternetAddress.parse("recipient@gmail.com"))
            setSubject("Test subject")

            val multipart = javax.mail.internet.MimeMultipart()
            val textBodyPart = javax.mail.internet.MimeBodyPart().apply {
                setText("Test body", "UTF-8")
            }
            multipart.addBodyPart(textBodyPart)

            // Mock file creation for attachment
            val mockFile = File.createTempFile("mock_img", ".png")
            mockFile.writeText("fake image bytes")

            val imagePart = javax.mail.internet.MimeBodyPart()
            val dataSource = javax.activation.FileDataSource(mockFile)
            imagePart.dataHandler = javax.activation.DataHandler(dataSource)
            imagePart.fileName = mockFile.name
            multipart.addBodyPart(imagePart)

            setContent(multipart)
            saveChanges()

            mockFile.delete()
        }

        assert(message.subject == "Test subject")
        val content = message.content
        assert(content is javax.mail.internet.MimeMultipart)
        val multipart = content as javax.mail.internet.MimeMultipart
        assert(multipart.count == 2)
    }

    @Test
    fun testReadExcelColumns() {
        val file = File("src/main/res/raw/email.xlsx")
        try {
            file.inputStream().use { inputStream ->
                val workbook = WorkbookFactory.create(inputStream)
                val sheet = workbook.getSheetAt(0)
                var count = 0
                for (row in sheet) {
                    print("Row ${row.rowNum}: ")
                    for (c in 0 until row.lastCellNum) {
                        val cell = row.getCell(c)
                        print("Col $c: '$cell' | ")
                    }
                    println()
                    count++
                    if (count >= 10) break
                }
                workbook.close()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
