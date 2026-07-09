package com.easystyleshop.massengerapp.util

import java.util.Properties
import java.util.concurrent.atomic.AtomicInteger
import javax.mail.*
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage
import kotlin.concurrent.thread

object SmtpEmailSender {

    private val successCount = AtomicInteger(0)

    fun sendEmail(
        senderEmail: String,
        senderPassword: String,
        recipientEmail: String,
        subject: String,
        body: String,
        onSuccess: (sentCount: Int) -> Unit,
        onError: (Exception) -> Unit
    ) {
        thread {
            try {
                val props = Properties().apply {
                    put("mail.smtp.auth", "true")
                    put("mail.smtp.starttls.enable", "true")
                    put("mail.smtp.host", "smtp.gmail.com")
                    put("mail.smtp.port", "587")
                }

                val session = Session.getInstance(props, object : Authenticator() {
                    override fun getPasswordAuthentication(): PasswordAuthentication {
                        return PasswordAuthentication(senderEmail, senderPassword)
                    }
                })

                val message = MimeMessage(session).apply {
                    setFrom(InternetAddress(senderEmail))
                    setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipientEmail))
                    setSubject(subject)
                    setText(body)
                }

                Transport.send(message)

                val count = successCount.incrementAndGet()
                onSuccess(count)

            } catch (e: Exception) {
                onError(e)
            }
        }
    }

}
