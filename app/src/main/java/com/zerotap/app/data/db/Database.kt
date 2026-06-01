package com.zerotap.app.data.db

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "memories")
data class MemoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val kind: String,
    val key: String,
    val value: String,
    val weight: Double,
    val pinned: Boolean,
    val locked: Boolean,
    val useCount: Int,
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(tableName = "task_history")
data class TaskRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val goal: String,
    val taskType: String,
    val status: String,
    val summary: String,
    val createdAt: Long,
    val finishedAt: Long
)

@Dao
interface MemoryDao {
    @Query("SELECT * FROM memories ORDER BY pinned DESC, weight DESC, updatedAt DESC")
    fun observeAll(): Flow<List<MemoryEntity>>

    @Query("SELECT * FROM memories WHERE kind = :kind ORDER BY weight DESC")
    suspend fun byKind(kind: String): List<MemoryEntity>

    @Query("SELECT * FROM memories ORDER BY pinned DESC, weight DESC, useCount DESC LIMIT :limit")
    suspend fun top(limit: Int): List<MemoryEntity>

    @Query("SELECT * FROM memories WHERE kind = :kind AND key = :key LIMIT 1")
    suspend fun find(kind: String, key: String): MemoryEntity?

    @Query("SELECT COUNT(*) FROM memories")
    suspend fun count(): Int

    @Query(
        "SELECT * FROM memories WHERE key LIKE '%' || :q || '%' " +
            "OR value LIKE '%' || :q || '%' ORDER BY weight DESC"
    )
    suspend fun search(q: String): List<MemoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: MemoryEntity): Long

    @Update
    suspend fun update(item: MemoryEntity)

    @Query("UPDATE memories SET pinned = :pinned, updatedAt = :ts WHERE id = :id")
    suspend fun setPinned(id: Long, pinned: Boolean, ts: Long)

    @Query("UPDATE memories SET locked = :locked, updatedAt = :ts WHERE id = :id")
    suspend fun setLocked(id: Long, locked: Boolean, ts: Long)

    @Query("UPDATE memories SET value = :value, updatedAt = :ts WHERE id = :id")
    suspend fun editValue(id: Long, value: String, ts: Long)

    @Query("DELETE FROM memories WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface TaskDao {
    @Query("SELECT * FROM task_history ORDER BY createdAt DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<TaskRecordEntity>>

    @Insert
    suspend fun insert(item: TaskRecordEntity): Long

    @Update
    suspend fun update(item: TaskRecordEntity)

    @Query("UPDATE task_history SET status = :status, summary = :summary, finishedAt = :ts WHERE id = :id")
    suspend fun finish(id: Long, status: String, summary: String, ts: Long)

    @Query("DELETE FROM task_history")
    suspend fun clear()
}

@Database(
    entities = [MemoryEntity::class, TaskRecordEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun memoryDao(): MemoryDao
    abstract fun taskDao(): TaskDao

    companion object {
        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "zerotap.db")
                .fallbackToDestructiveMigration()
                .build()
    }
}
