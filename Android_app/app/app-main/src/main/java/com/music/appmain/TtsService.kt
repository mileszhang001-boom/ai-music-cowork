package com.music.appmain

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

sealed class TtsState {
    object Idle : TtsState()
    object Initializing : TtsState()
    object Ready : TtsState()
    data class Speaking(val text: String) : TtsState()
    object Error : TtsState()
}

sealed class TtsInitResult {
    object Success : TtsInitResult()
    data class Error(val message: String) : TtsInitResult()
}

interface TtsCallback {
    fun onInitSuccess()
    fun onInitFailed(message: String)
    fun onSpeakStart(text: String)
    fun onSpeakDone()
    fun onError(message: String)
}

class TtsService(context: Context) {

    private val appContext = context.applicationContext

    private var tts: TextToSpeech? = null

    private val _state = MutableStateFlow<TtsState>(TtsState.Idle)
    val state: StateFlow<TtsState> = _state.asStateFlow()

    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()

    private var callback: TtsCallback? = null

    private val utteranceIdCounter = java.util.concurrent.atomic.AtomicInteger(0)

    private val ttsInitListener = TextToSpeech.OnInitListener { status ->
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.CHINESE)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                _state.value = TtsState.Error
                _isInitialized.value = false
                callback?.onInitFailed("中文语言不支持或数据缺失")
            } else {
                setupUtteranceProgressListener()
                _state.value = TtsState.Ready
                _isInitialized.value = true
                callback?.onInitSuccess()
            }
        } else {
            _state.value = TtsState.Error
            _isInitialized.value = false
            callback?.onInitFailed("TTS 初始化失败，状态码: $status")
        }
    }

    fun init() {
        if (_isInitialized.value) {
            return
        }
        _state.value = TtsState.Initializing
        tts = TextToSpeech(appContext, ttsInitListener)
    }

    fun setCallback(callback: TtsCallback?) {
        this.callback = callback
    }

    private fun setupUtteranceProgressListener() {
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                val currentText = _state.value.let { state ->
                    if (state is TtsState.Speaking) state.text else ""
                }
                _state.value = TtsState.Speaking(currentText)
                callback?.onSpeakStart(currentText)
            }

            override fun onDone(utteranceId: String?) {
                _state.value = TtsState.Ready
                callback?.onSpeakDone()
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                _state.value = TtsState.Error
                callback?.onError("播放过程中发生错误")
            }

            override fun onError(utteranceId: String?, errorCode: Int) {
                _state.value = TtsState.Error
                callback?.onError("播放错误，错误码: $errorCode")
            }
        })
    }

    fun speak(text: String): Boolean {
        if (!_isInitialized.value) {
            callback?.onError("TTS 尚未初始化")
            return false
        }

        if (text.isBlank()) {
            callback?.onError("播放文本为空")
            return false
        }

        val utteranceId = "utterance_${utteranceIdCounter.incrementAndGet()}"
        val result = tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
        
        if (result == TextToSpeech.ERROR) {
            _state.value = TtsState.Error
            callback?.onError("播放失败")
            return false
        }

        _state.value = TtsState.Speaking(text)
        return true
    }

    fun speakQueue(text: String): Boolean {
        if (!_isInitialized.value) {
            callback?.onError("TTS 尚未初始化")
            return false
        }

        if (text.isBlank()) {
            callback?.onError("播放文本为空")
            return false
        }

        val utteranceId = "utterance_${utteranceIdCounter.incrementAndGet()}"
        val result = tts?.speak(text, TextToSpeech.QUEUE_ADD, null, utteranceId)
        
        if (result == TextToSpeech.ERROR) {
            _state.value = TtsState.Error
            callback?.onError("播放失败")
            return false
        }

        return true
    }

    fun stop() {
        tts?.stop()
        _state.value = TtsState.Ready
    }

    fun shutdown() {
        stop()
        tts?.shutdown()
        tts = null
        _state.value = TtsState.Idle
        _isInitialized.value = false
        callback = null
    }

    fun setSpeechRate(rate: Float) {
        tts?.setSpeechRate(rate.coerceIn(0.5f, 2.0f))
    }

    fun setPitch(pitch: Float) {
        tts?.setPitch(pitch.coerceIn(0.5f, 2.0f))
    }

    fun isSpeaking(): Boolean {
        return tts?.isSpeaking ?: false
    }
}
