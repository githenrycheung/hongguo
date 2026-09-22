package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {

    // Play History
    @Query("SELECT * FROM play_history ORDER BY updatedTimestamp DESC")
    fun getAllPlayHistory(): Flow<List<PlayHistoryEntity>>

    @Query("SELECT * FROM play_history WHERE dramaId = :dramaId LIMIT 1")
    suspend fun getPlayHistory(dramaId: String): PlayHistoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdatePlayHistory(item: PlayHistoryEntity)

    @Query("DELETE FROM play_history WHERE dramaId = :dramaId")
    suspend fun deletePlayHistory(dramaId: String)

    @Query("DELETE FROM play_history")
    suspend fun clearAllPlayHistory()

    // Clipboard History
    @Query("SELECT * FROM clipboard_history ORDER BY timestamp DESC LIMIT 50")
    fun getAllClipboardHistory(): Flow<List<ClipboardHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClipboardHistory(item: ClipboardHistoryEntity)

    @Query("DELETE FROM clipboard_history WHERE id = :id")
    suspend fun deleteClipboardHistory(id: Long)

    @Query("DELETE FROM clipboard_history")
    suspend fun clearAllClipboardHistory()
}
