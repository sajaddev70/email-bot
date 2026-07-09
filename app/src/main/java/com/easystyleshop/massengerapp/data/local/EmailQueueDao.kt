package com.easystyleshop.massengerapp.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.easystyleshop.massengerapp.data.model.EmailQueueItem

@Dao
interface EmailQueueDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(emails: List<EmailQueueItem>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(email: EmailQueueItem)

    @Query("SELECT * FROM email_queue WHERE status = 'PENDING' ORDER BY id ASC")
    suspend fun getAllPending(): List<EmailQueueItem>

    @Query("DELETE FROM email_queue WHERE email = :email")
    suspend fun deleteByEmail(email: String)

    @Query("DELETE FROM email_queue WHERE id = :id")
    suspend fun deleteById(id: Int)

    @Query("DELETE FROM email_queue")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM email_queue WHERE status = 'PENDING'")
    suspend fun getPendingCount(): Int
}
