package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversationDao {
    @Query("SELECT * FROM conversations ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<ConversationEntity>>

    @Query("SELECT * FROM conversations WHERE id = :id LIMIT 1")
    fun observeById(id: String): Flow<ConversationEntity?>

    @Query("SELECT * FROM conversations WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): ConversationEntity?

    @Query("SELECT * FROM conversations")
    suspend fun getAll(): List<ConversationEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<ConversationEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: ConversationEntity)

    @Query("UPDATE conversations SET title = :title WHERE id = :id")
    suspend fun updateTitle(id: String, title: String)

    @Query("UPDATE conversations SET sandboxStatus = :sandboxStatus WHERE id = :id")
    suspend fun updateSandboxStatus(id: String, sandboxStatus: String)

    @Query("UPDATE conversations SET sandboxStatus = :sandboxStatus WHERE sandboxId = :sandboxId")
    suspend fun updateSandboxStatusBySandbox(sandboxId: String, sandboxStatus: String)

    /** Removes rows that were not touched by the sync that started at [syncStartedAt]. */
    @Query("DELETE FROM conversations WHERE syncedAt < :syncStartedAt")
    suspend fun deleteSyncedBefore(syncStartedAt: Long)

    @Query("DELETE FROM conversations WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM conversations")
    suspend fun clearAll()
}

@Dao
interface EventDao {
    @Query("SELECT * FROM events WHERE conversationId = :conversationId ORDER BY timestamp ASC, id ASC")
    fun observeForConversation(conversationId: String): Flow<List<AgentEventEntity>>

    @Query("SELECT * FROM events WHERE conversationId = :conversationId AND timestampIso IS NOT NULL ORDER BY timestamp DESC, id DESC LIMIT 1")
    suspend fun latestForConversation(conversationId: String): AgentEventEntity?

    @Query("SELECT COUNT(*) FROM events WHERE conversationId = :conversationId")
    suspend fun countForConversation(conversationId: String): Int

    @Query("SELECT * FROM events WHERE conversationId = :conversationId ORDER BY timestamp ASC, id ASC")
    suspend fun snapshotForConversation(conversationId: String): List<AgentEventEntity>

    @Query("DELETE FROM events WHERE id = :id")
    suspend fun deleteById(id: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(events: List<AgentEventEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(event: AgentEventEntity)

    @Query("DELETE FROM events WHERE conversationId = :conversationId")
    suspend fun deleteForConversation(conversationId: String)

    @Query("DELETE FROM events")
    suspend fun clearAll()
}

@Dao
interface TemplateDao {
    @Query("SELECT * FROM templates ORDER BY category, title")
    fun observeAll(): Flow<List<SavedTemplateEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(templates: List<SavedTemplateEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(template: SavedTemplateEntity)

    @Query("DELETE FROM templates WHERE id = :id")
    suspend fun delete(id: String)

    @Query("SELECT COUNT(*) FROM templates")
    suspend fun count(): Int
}
