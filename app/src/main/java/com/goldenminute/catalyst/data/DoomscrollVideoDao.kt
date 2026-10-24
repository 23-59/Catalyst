package com.goldenminute.catalyst.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface DoomscrollVideoDao {
    @Query("SELECT * FROM doomscroll_videos ORDER BY timestamp DESC")
    fun getAllRecords(): Flow<List<DoomscrollVideoRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: DoomscrollVideoRecord): Long

    @Query("SELECT SUM(durationMs) FROM doomscroll_videos WHERE timestamp >= :sinceTimestamp")
    suspend fun getTotalDurationSince(sinceTimestamp: Long): Long?

    @Query("DELETE FROM doomscroll_videos")
    suspend fun clearAllRecords()
}
