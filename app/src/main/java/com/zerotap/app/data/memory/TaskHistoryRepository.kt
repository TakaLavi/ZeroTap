package com.zerotap.app.data.memory

import com.zerotap.app.data.db.TaskDao
import com.zerotap.app.data.db.TaskRecordEntity
import kotlinx.coroutines.flow.Flow

/** Stores a lightweight record of every mission the agent has attempted. */
class TaskHistoryRepository(private val dao: TaskDao) {

    fun recent(limit: Int = 30): Flow<List<TaskRecordEntity>> = dao.observeRecent(limit)

    suspend fun start(goal: String, taskType: String): Long = dao.insert(
        TaskRecordEntity(
            goal = goal,
            taskType = taskType,
            status = "RUNNING",
            summary = "",
            createdAt = System.currentTimeMillis(),
            finishedAt = 0L
        )
    )

    suspend fun finish(id: Long, status: String, summary: String) {
        if (id <= 0) return
        dao.finish(id, status, summary, System.currentTimeMillis())
    }

    suspend fun clear() = dao.clear()
}
