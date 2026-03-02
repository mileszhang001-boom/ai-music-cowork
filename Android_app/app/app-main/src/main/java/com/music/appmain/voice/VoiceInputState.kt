package com.music.appmain.voice

sealed class VoiceInputState {
    object Idle : VoiceInputState()
    data class Listening(val timestamp: Long = System.currentTimeMillis()) : VoiceInputState()
    data class Processing(val amplitude: Int = 0) : VoiceInputState()
    data class Result(val text: String) : VoiceInputState()
    data class Error(val message: String) : VoiceInputState()
}
