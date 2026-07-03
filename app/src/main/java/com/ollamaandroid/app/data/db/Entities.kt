package com.ollamaandroid.app.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val model: String,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(
    tableName = "messages",
    foreignKeys = [
        ForeignKey(
            entity = ConversationEntity::class,
            parentColumns = ["id"],
            childColumns = ["conversationId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index("conversationId")],
)
data class MessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val conversationId: Long,
    /** "user" or "assistant" */
    val role: String,
    val content: String,
    /** Reasoning trace emitted by thinking-capable models; null when absent. */
    val thinking: String? = null,
    /** Model that produced an assistant message; null for user messages. */
    val model: String? = null,
    /** True when a stream was stopped or failed before completing. */
    val interrupted: Boolean = false,
    val createdAt: Long,
)
