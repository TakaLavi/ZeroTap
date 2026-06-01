package com.zerotap.app.data.memory

import com.zerotap.app.core.MemoryItem
import com.zerotap.app.core.MemoryKind
import com.zerotap.app.data.db.MemoryDao
import com.zerotap.app.data.db.MemoryEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Reads, writes and ranks the durable preferences the agent learns about the user. */
class MemoryRepository(private val dao: MemoryDao) {

    val all: Flow<List<MemoryItem>> = dao.observeAll().map { list -> list.map { it.toDomain() } }

    /** Reinforce or create a memory. Locked memories are never overwritten automatically. */
    suspend fun remember(
        kind: MemoryKind,
        key: String,
        value: String,
        weightDelta: Double = 1.0
    ) {
        val now = System.currentTimeMillis()
        val existing = dao.find(kind.name, key.trim())
        if (existing != null) {
            if (existing.locked) return
            dao.update(
                existing.copy(
                    value = value,
                    weight = existing.weight + weightDelta,
                    useCount = existing.useCount + 1,
                    updatedAt = now
                )
            )
        } else {
            dao.upsert(
                MemoryEntity(
                    kind = kind.name,
                    key = key.trim(),
                    value = value,
                    weight = weightDelta,
                    pinned = false,
                    locked = false,
                    useCount = 1,
                    createdAt = now,
                    updatedAt = now
                )
            )
        }
    }

    /** A compact, model-ready summary of the strongest preferences. */
    suspend fun contextBlock(limit: Int = 24): String {
        val items = dao.top(limit)
        if (items.isEmpty()) return "No stored preferences yet."
        return items.joinToString("\n") { e ->
            "- [${e.kind.lowercase()}] ${e.key}: ${e.value}"
        }
    }

    suspend fun search(query: String): List<MemoryItem> =
        if (query.isBlank()) emptyList() else dao.search(query.trim()).map { it.toDomain() }

    suspend fun setPinned(id: Long, pinned: Boolean) =
        dao.setPinned(id, pinned, System.currentTimeMillis())

    suspend fun setLocked(id: Long, locked: Boolean) =
        dao.setLocked(id, locked, System.currentTimeMillis())

    suspend fun edit(id: Long, value: String) =
        dao.editValue(id, value, System.currentTimeMillis())

    suspend fun delete(item: MemoryItem) = dao.delete(item.id)

    suspend fun seedIfEmpty() {
        if (dao.count() > 0) return
        val now = System.currentTimeMillis()
        DEMO_SEED.forEach { (kind, key, value) ->
            dao.upsert(
                MemoryEntity(
                    kind = kind.name, key = key, value = value,
                    weight = 2.0, pinned = false, locked = false,
                    useCount = 1, createdAt = now, updatedAt = now
                )
            )
        }
    }

    private fun MemoryEntity.toDomain(): MemoryItem = MemoryItem(
        id = id,
        kind = runCatching { MemoryKind.valueOf(kind) }.getOrDefault(MemoryKind.PREFERENCE),
        key = key,
        value = value,
        weight = weight,
        pinned = pinned,
        locked = locked,
        useCount = useCount,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    private companion object {
        val DEMO_SEED = listOf(
            Triple(MemoryKind.PREFERENCE, "budget", "Prefers cheap, good-value options"),
            Triple(MemoryKind.FOOD, "cuisine", "Loves spicy ramen and street food"),
            Triple(MemoryKind.BRAND, "shoes", "Tends to pick Nike and Adidas"),
            Triple(MemoryKind.STORE, "shopping", "Checks Amazon first, then Flipkart"),
            Triple(MemoryKind.PERSON, "Bethany", "Frequent contact on Discord")
        )
    }
}
