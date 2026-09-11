package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversationDao {
    @Query("SELECT * FROM conversations ORDER BY updatedAt DESC")
    fun getAllConversations(): Flow<List<ConversationEntity>>

    @Query("SELECT * FROM conversations WHERE id = :id LIMIT 1")
    fun getConversationById(id: String): Flow<ConversationEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversations(items: List<ConversationEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversation(item: ConversationEntity)

    @Query("DELETE FROM conversations WHERE id = :id")
    suspend fun deleteConversation(id: String)

    @Query("DELETE FROM conversations")
    suspend fun clearAll()
}

@Dao
interface EventDao {
    @Query("SELECT * FROM events WHERE conversationId = :conversationId ORDER BY timestamp ASC")
    fun getEventsForConversation(conversationId: String): Flow<List<AgentEventEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvents(events: List<AgentEventEntity>)

    @Query("DELETE FROM events WHERE conversationId = :conversationId")
    suspend fun deleteEventsForConversation(conversationId: String)

    @Query("DELETE FROM events")
    suspend fun clearAll()
}

@Dao
interface TemplateDao {
    @Query("SELECT * FROM templates ORDER BY category, title")
    fun getAllTemplates(): Flow<List<SavedTemplateEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplates(templates: List<SavedTemplateEntity>)

    @Query("SELECT COUNT(*) FROM templates")
    suspend fun count(): Int
}
