package com.manalejandro.mitension.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {

    @Query("SELECT * FROM users ORDER BY isPrimary DESC, createdAt ASC")
    fun observeAll(): Flow<List<User>>

    @Query("SELECT * FROM users WHERE id = :id")
    suspend fun getById(id: Long): User?

    @Query("SELECT * FROM users WHERE isPrimary = 1 LIMIT 1")
    suspend fun getPrimary(): User?

    @Query("SELECT COUNT(*) FROM users")
    suspend fun count(): Int

    @Insert
    suspend fun insert(user: User): Long

    @Update
    suspend fun update(user: User)

    @Delete
    suspend fun delete(user: User)
}

@Dao
interface ReadingDao {

    @Query("SELECT * FROM readings WHERE userId = :userId ORDER BY timestamp DESC")
    fun observeByUser(userId: Long): Flow<List<Reading>>

    @Query("SELECT * FROM readings WHERE id = :id")
    suspend fun getById(id: Long): Reading?

    @Query("SELECT COUNT(*) FROM readings WHERE userId = :userId")
    suspend fun countByUser(userId: Long): Int

    @Insert
    suspend fun insert(reading: Reading): Long

    @Update
    suspend fun update(reading: Reading)

    @Delete
    suspend fun delete(reading: Reading)
}

@Dao
interface ReminderDao {

    @Query("SELECT * FROM reminders ORDER BY CASE kind WHEN 'morning' THEN 0 WHEN 'midday' THEN 1 ELSE 2 END")
    fun observeAll(): Flow<List<Reminder>>

    @Query("SELECT * FROM reminders")
    suspend fun getAll(): List<Reminder>

    @Query("SELECT * FROM reminders WHERE kind = :kind")
    suspend fun getByKind(kind: String): Reminder?

    @Insert
    suspend fun insert(reminder: Reminder)

    @Update
    suspend fun update(reminder: Reminder)
}