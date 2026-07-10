package com.easystyleshop.massengerapp.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.easystyleshop.massengerapp.data.model.EmailQueueItem
import com.easystyleshop.massengerapp.data.model.SenderAccountStats
import kotlinx.coroutines.flow.Flow

@Dao
interface EmailQueueDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(emails: List<EmailQueueItem>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(email: EmailQueueItem)

    @Update
    suspend fun update(email: EmailQueueItem)

    @Query("SELECT * FROM email_queue WHERE status = 'PENDING' ORDER BY id ASC")
    suspend fun getAllPending(): List<EmailQueueItem>

    @Query("SELECT * FROM email_queue ORDER BY id ASC")
    suspend fun getAllItems(): List<EmailQueueItem>

    @Query("DELETE FROM email_queue WHERE email = :email")
    suspend fun deleteByEmail(email: String)

    @Query("DELETE FROM email_queue WHERE id = :id")
    suspend fun deleteById(id: Int)

    @Query("DELETE FROM email_queue")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM email_queue WHERE status = 'PENDING'")
    suspend fun getPendingCount(): Int

    @Query("SELECT COUNT(*) FROM email_queue WHERE status = 'SENT'")
    suspend fun getSentCount(): Int

    @Query("SELECT COUNT(*) FROM email_queue WHERE status = 'INVALID_FORMAT'")
    suspend fun getInvalidFormatCount(): Int

    @Query("SELECT COUNT(*) FROM email_queue WHERE status = 'SMTP_REJECTED'")
    suspend fun getSmtpRejectedCount(): Int

    @Query("SELECT COUNT(*) FROM email_queue WHERE status = 'PENDING'")
    fun getPendingCountFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM email_queue WHERE status = 'SENT'")
    fun getSentCountFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM email_queue WHERE status = 'INVALID_FORMAT'")
    fun getInvalidFormatCountFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM email_queue WHERE status = 'SMTP_REJECTED'")
    fun getSmtpRejectedCountFlow(): Flow<Int>

    @Query("SELECT senderEmail, COUNT(*) as count FROM email_queue WHERE status = 'SENT' AND senderEmail IS NOT NULL GROUP BY senderEmail")
    suspend fun getSenderStats(): List<SenderAccountStats>
}
