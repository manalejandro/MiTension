package com.manalejandro.mitension.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val isPrimary: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "readings",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("userId")]
)
data class Reading(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long,
    val systolic: Double,
    val diastolic: Double,
    val pulse: Double,
    val note: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "reminders")
data class Reminder(
    @PrimaryKey val kind: String,
    val enabled: Boolean = false,
    val hour: Int = 9,
    val minute: Int = 0,
    val soundUri: String? = null
)