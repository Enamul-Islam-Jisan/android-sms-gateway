package me.capcom.smsgateway.modules.webhooks.db

import androidx.room.*

import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for webhook queue operations.
 */
@Dao
interface WebhookQueueDao {

    /**
     * Insert a new webhook event into the queue.
     */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertWebhook(webhook: WebhookQueueEntity)

    /**
     * Get webhook by id.
     */
    @Query("SELECT * FROM webhook_queue WHERE id = :id")
    suspend fun getById(id: String): WebhookQueueEntity

    /**
     * Check if there are any scheduled webhook events.
     */
    @Query("SELECT COUNT(*) FROM webhook_queue WHERE status IN ('pending', 'failed')")
    suspend fun scheduledWebhooksCount(): Long

    /**
     * Get all pending webhook events ordered by next attempt time.
     */
    @Query(
        """
        SELECT * FROM webhook_queue 
        WHERE status IN ("pending", "failed") AND next_attempt <= :currentTime 
        ORDER BY next_attempt ASC 
        LIMIT :limit
    """
    )
    suspend fun getPendingWebhooks(
        currentTime: Long = System.currentTimeMillis(),
        limit: Int = 10
    ): List<WebhookQueueEntity>

    /**
     * Get all webhook events ordered by creation time.
     */
    @Query("SELECT * FROM webhook_queue ORDER BY created_at DESC")
    fun selectQueue(): Flow<List<WebhookQueueEntity>>

    /**
     * Get pending webhook events.
     */
    @Query("SELECT * FROM webhook_queue WHERE status IN ('pending', 'processing') ORDER BY created_at DESC")
    fun selectPendingQueue(): Flow<List<WebhookQueueEntity>>

    /**
     * Get failed webhook events.
     */
    @Query("SELECT * FROM webhook_queue WHERE status IN ('failed', 'permanently_failed') ORDER BY created_at DESC")
    fun selectFailedQueue(): Flow<List<WebhookQueueEntity>>

    /**
     * Get completed webhook events with a limit.
     */
    @Query("SELECT * FROM webhook_queue WHERE status IN ('completed', 'cancelled') ORDER BY created_at DESC LIMIT :limit")
    fun selectHistoryQueue(limit: Int): Flow<List<WebhookQueueEntity>>

    /**
     * Delete a webhook by ID.
     */
    @Query("DELETE FROM webhook_queue WHERE id = :id")
    suspend fun delete(id: String)

    /**
     * Update webhook status.
     */
    @Query("UPDATE webhook_queue SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: String, status: WebhookStatus)

    /**
     * Mark webhook as processing.
     */
    @Query(
        """
        UPDATE webhook_queue 
        SET status = "processing" 
        WHERE id = :id
    """
    )
    suspend fun markAsProcessing(
        id: String,
    )

    /**
     * Update retry information and set next attempt time.
     */
    @Query(
        """
        UPDATE webhook_queue 
        SET status = "failed", 
            retry_count = retry_count + 1, 
            next_attempt = :nextAttempt,
            last_error = :error
        WHERE id = :id
    """
    )
    suspend fun markAsFailed(
        id: String,
        nextAttempt: Long,
        error: String?,
    )

    /**
     * Mark webhook as completed.
     */
    @Query(
        """
        UPDATE webhook_queue 
        SET status = "completed" 
        WHERE id = :id
    """
    )
    suspend fun markAsCompleted(
        id: String,
    )

    /**
     * Mark webhook as permanently failed.
     */
    @Query(
        """
        UPDATE webhook_queue 
        SET status = "permanently_failed", last_error = :error
        WHERE id = :id
    """
    )
    suspend fun markAsPermanentlyFailed(
        id: String,
        error: String,
    )

    /**
     * Get queue statistics.
     */
    @Query(
        """
        SELECT 
            COUNT(*) as total,
            SUM(CASE WHEN status = 'pending' THEN 1 ELSE 0 END) as pending,
            SUM(CASE WHEN status = 'processing' THEN 1 ELSE 0 END) as processing,
            SUM(CASE WHEN status = 'failed' THEN 1 ELSE 0 END) as failed,
            SUM(CASE WHEN status = 'permanently_failed' THEN 1 ELSE 0 END) as permanentlyFailed,
            SUM(CASE WHEN status = 'completed' THEN 1 ELSE 0 END) as completed,
            SUM(CASE WHEN status = 'cancelled' THEN 1 ELSE 0 END) as cancelled
        FROM webhook_queue
    """
    )
    fun getQueueStatistics(): Flow<WebhookQueueStatistics>

    /**
     * Get IDs of old completed/permanently failed webhook entries
     */
    @Query("SELECT id FROM webhook_queue WHERE status IN ('completed', 'permanently_failed', 'cancelled') AND created_at < :cutoffTime")
    suspend fun getOldEntryIds(cutoffTime: Long): List<String>

    /**
     * Get IDs of completed/cancelled webhook entries
     */
    @Query("SELECT id FROM webhook_queue WHERE status IN ('completed', 'cancelled')")
    suspend fun getHistoryIds(): List<String>

    /**
     * Clean up old completed webhook events.
     */
    @Query(
        """
        DELETE FROM webhook_queue 
        WHERE id IN (:ids)
    """
    )
    suspend fun cleanupOldEntries(ids: List<String>)

    /**
     * Recover stuck processing webhooks (timed out workers).
     */
    @Query(
        """
        UPDATE webhook_queue 
        SET status = "pending" 
        WHERE status = "processing" AND next_attempt < :timeoutThreshold
    """
    )
    suspend fun recoverStuckProcessingWebhooks(
        timeoutThreshold: Long = System.currentTimeMillis() - (5 * 60 * 1000) // 5 minutes
    )
}

/**
 * Data class for queue statistics.
 */
data class WebhookQueueStatistics(
    val total: Int,
    val pending: Int,
    val processing: Int,
    val failed: Int,
    val permanentlyFailed: Int,
    val completed: Int,
    val cancelled: Int
)
