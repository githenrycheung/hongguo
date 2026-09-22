package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "play_history")
data class PlayHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val dramaId: String,
    val dramaTitle: String,
    val episodeIndex: Int,
    val totalEpisodes: Int,
    val videoUrl: String,
    val coverUrl: String,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val updatedTimestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "clipboard_history")
data class ClipboardHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val rawText: String,
    val extractedUrl: String,
    val parsedTitle: String,
    val episodeIndex: Int,
    val videoUrl: String,
    val timestamp: Long = System.currentTimeMillis()
)
