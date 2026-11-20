package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class CheckStatus {
    SUCCESS,
    EMPTY,
    ERROR
}

@Entity(tableName = "check_logs")
data class CheckLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val checkedAt: Long,
    val status: String,
    val totalFound: Int,
    val newCount: Int,
    val triggerSource: String,
    val message: String
)
