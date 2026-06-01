package com.zerotap.app.logging

import com.zerotap.app.core.ActionRecord
import com.zerotap.app.core.ActionType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.concurrent.atomic.AtomicLong

/** Process-wide rolling log of everything the agent does, surfaced in the debug viewer. */
object TraceLog {

    private const val MAX = 250
    private val counter = AtomicLong(0)

    private val _records = MutableStateFlow<List<ActionRecord>>(emptyList())
    val records: StateFlow<List<ActionRecord>> = _records

    fun log(actionType: ActionType, target: String, success: Boolean, detail: String) {
        val record = ActionRecord(
            id = counter.incrementAndGet(),
            timestamp = System.currentTimeMillis(),
            actionType = actionType,
            target = target,
            success = success,
            detail = detail
        )
        _records.value = (_records.value + record).takeLast(MAX)
    }

    fun clear() {
        _records.value = emptyList()
    }
}
