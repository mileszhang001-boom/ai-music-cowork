package com.music.appmain.voice

interface VoiceInputCallback {
    fun onReadyForSpeech()
    fun onBeginningOfSpeech()
    fun onRmsChanged(rmsdB: Float)
    fun onEndOfSpeech()
    fun onError(error: Int)
    fun onResult(text: String)
    fun onVoiceError(message: String)
}
