package com.example.layer3.api.model

object EngineTypes {
    const val CONTENT = "content"
    const val LIGHTING = "lighting"
    const val AUDIO = "audio"
}

object EffectStatus {
    const val PENDING = "pending"
    const val EXECUTING = "executing"
    const val COMPLETED = "completed"
    const val FAILED = "failed"
}

data class EffectCommand(
    val command_id: String,
    val engine_type: String,
    val action: String,
    val params: Map<String, Any> = emptyMap(),
    val priority: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
)

data class EffectResult(
    val command_id: String,
    val status: String,
    val result_data: Map<String, Any>? = null,
    val error_message: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

data class EffectCommands(
    val commands: List<EffectCommand> = emptyList(),
    val sequence_id: String = "",
    val scene_id: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
