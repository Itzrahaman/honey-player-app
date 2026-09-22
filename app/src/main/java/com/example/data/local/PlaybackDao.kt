package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaybackDao {
    @Query("SELECT * FROM playback_records WHERE videoUri = :videoUri LIMIT 1")
    fun getRecord(videoUri: String): Flow<PlaybackRecord?>

    @Query("SELECT * FROM playback_records WHERE videoUri = :videoUri LIMIT 1")
    suspend fun getRecordSync(videoUri: String): PlaybackRecord?

    @Query("SELECT * FROM playback_records ORDER BY lastPlayedTimestamp DESC")
    fun getAllRecords(): Flow<List<PlaybackRecord>>

    @Query("SELECT videoUri FROM playback_records WHERE isFavorite = 1")
    fun getFavoriteUris(): Flow<List<String>>

    @Query("SELECT * FROM playback_records WHERE positionMs > 3000 AND (durationMs = 0 OR positionMs < (durationMs - 5000)) ORDER BY lastPlayedTimestamp DESC")
    fun getContinueWatchingRecords(): Flow<List<PlaybackRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRecord(record: PlaybackRecord)

    @Query("UPDATE playback_records SET positionMs = :positionMs, durationMs = :durationMs, lastPlayedTimestamp = :timestamp WHERE videoUri = :videoUri")
    suspend fun updatePosition(videoUri: String, positionMs: Long, durationMs: Long, timestamp: Long)

    @Query("UPDATE playback_records SET isFavorite = :isFavorite WHERE videoUri = :videoUri")
    suspend fun updateFavorite(videoUri: String, isFavorite: Boolean)

    @Query("DELETE FROM playback_records WHERE videoUri = :videoUri")
    suspend fun deleteRecord(videoUri: String)

    @Query("DELETE FROM playback_records")
    suspend fun clearAll()
}
