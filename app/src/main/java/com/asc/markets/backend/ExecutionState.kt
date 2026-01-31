
package com.asc.markets.backend

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
enum class ExecutionMode { 
    @SerialName("ASSISTANT") ASSISTANT, 
    @SerialName("AUTO") AUTO 
}

@Serializable
data class ExecutionState(
    @SerialName("mode") val mode: ExecutionMode = ExecutionMode.ASSISTANT,
    @SerialName("auto_execution_enabled") val autoExecutionEnabled: Boolean = false,
    @SerialName("execution_armed") val executionArmed: Boolean = false,
    @SerialName("execution_reason") val executionReason: String = "default_safety_lock",
    @SerialName("execution_expires_at") val executionExpiresAt: String? = null
)

/**
 * Adapter to translate between institutional JS/TS JSON and Kotlin types.
 * Parity: EXECUTION_STATE_CONTRACT
 */
object ExecutionStateAdapter {
    private val json = Json { ignoreUnknownKeys = true }

    fun fromJson(jsonString: String): ExecutionState {
        return json.decodeFromString(jsonString)
    }

    fun toJson(state: ExecutionState): String {
        return json.encodeToString(ExecutionState.serializer(), state)
    }
}
