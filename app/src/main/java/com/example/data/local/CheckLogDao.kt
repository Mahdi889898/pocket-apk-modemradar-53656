package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CheckLogDao {

    @Query("SELECT * FROM check_logs ORDER BY checkedAt DESC LIMIT 50")
    fun observeRecentLogs(): Flow<List<CheckLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: CheckLogEntity)

    @Query("DELETE FROM check_logs WHERE id NOT IN (SELECT id FROM check_logs ORDER BY checkedAt DESC LIMIT 50)")
    suspend fun trimOldLogs()

    @Query("DELETE FROM check_logs")
    suspend fun clearAll()
}
