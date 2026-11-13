package com.yawmi.app.data.repository

import com.yawmi.app.data.local.dao.TaskDao
import com.yawmi.app.data.local.entity.TaskEntity
import com.yawmi.domain.TaskItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class TaskRepository(private val taskDao: TaskDao) {

    fun observeOpen(): Flow<List<TaskItem>> = taskDao.observeOpen().map { list -> list.map { it.toDomain() } }

    suspend fun add(title: String, description: String?, now: Long = System.currentTimeMillis()) {
        taskDao.upsert(
            TaskEntity(
                id = UUID.randomUUID().toString(),
                title = title,
                description = description?.takeIf { it.isNotBlank() },
                createdAt = now,
                updatedAt = now,
            ),
        )
    }

    suspend fun setDone(id: String, done: Boolean, now: Long = System.currentTimeMillis()) =
        taskDao.setDone(id, done, now)
}

private fun TaskEntity.toDomain() = TaskItem(
    id = id,
    title = title,
    description = description,
    dueDateIso = dueDateIso,
    priority = priority,
    isDone = isDone,
)
