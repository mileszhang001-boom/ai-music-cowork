package com.example.layer1.sdk

import android.content.Context
import android.graphics.Bitmap
import android.util.Base64
import androidx.lifecycle.LifecycleOwner
import com.example.layer1.api.IPerceptionEngine
import com.example.layer1.api.Layer1Config
import com.example.layer1.api.data.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class PerceptionEngine(
    private val context: Context,
    private var config: Layer1Config,
    private val lifecycleOwner: LifecycleOwner
) : IPerceptionEngine {

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val _signalsFlow = MutableSharedFlow<StandardizedSignals>(replay = 1)
    override val standardizedSignalsFlow = _signalsFlow.asSharedFlow()

    private val sensorManager = SensorManager(context)
    private var aiClient = AIClient(config)
    private val weatherService = WeatherService()
    private val localImageAnalyzer = LocalImageAnalyzer()
    private val confidenceValidator = ConfidenceValidator(0.6)
    
    private val cameraSource = CameraSource(context, lifecycleOwner)
    private var ipCameraSource = IpCameraSource(config)

    private var currentBitmap: Bitmap? = null
    private var currentInternalBitmap: Bitmap? = null
    private var isRunning = false
    private var loopJob: Job? = null

    init {
        setupCallbacks()
    }

    private fun setupCallbacks() {
        cameraSource.setCallback(object : CameraSource.ImageCallback {
            override fun onBitmapAvailable(bitmap: Bitmap) {
                currentBitmap = bitmap
            }
        })
        setupIpCameraCallback()
    }

    private fun setupIpCameraCallback() {
        ipCameraSource.setCallback(object : IpCameraSource.FrameCallback {
            override fun onFrameAvailable(bitmap: Bitmap) {
                currentInternalBitmap = bitmap
            }
        })
    }

    override fun start() {
        if (isRunning) return
        isRunning = true
        
        sensorManager.startLocationUpdates()
        sensorManager.startAudio()
        cameraSource.start()
        ipCameraSource.start()
        
        loopJob = scope.launch {
            while (isActive && isRunning) {
                val loopStartTime = System.currentTimeMillis()
                try {
                    processFrame()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                val elapsedTime = System.currentTimeMillis() - loopStartTime
                val delayTime = (config.refreshIntervalMs - elapsedTime).coerceAtLeast(0)
                delay(delayTime)
            }
        }
    }

    private suspend fun processFrame() {
        val location = sensorManager.currentLocation
        val micData = sensorManager.getAudioMetrics()
        
        // External Camera
        val bitmapToProcess = currentBitmap ?: Bitmap.createBitmap(512, 512, Bitmap.Config.ARGB_8888).apply {
            eraseColor(android.graphics.Color.rgb((0..255).random(), (0..255).random(), (0..255).random()))
        }
        
        val outputStream = ByteArrayOutputStream()
        bitmapToProcess.compress(Bitmap.CompressFormat.JPEG, 50, outputStream)
        val imageBytes = outputStream.toByteArray()
        val imageBase64 = Base64.encodeToString(imageBytes, Base64.NO_WRAP)

        val localAnalysis = localImageAnalyzer.analyze(bitmapToProcess)
        val aiResult = aiClient.analyzeExternalCamera(imageBase64)
        
        val finalExternalSignal = aiResult.copy(
            primary_color = localAnalysis.primaryColor,
            secondary_color = localAnalysis.secondaryColor,
            brightness = localAnalysis.brightness
        )

        // Internal Camera
        val internalBitmap = currentInternalBitmap
        val internalSignal = if (internalBitmap != null) {
            val outputStreamInternal = ByteArrayOutputStream()
            internalBitmap.compress(Bitmap.CompressFormat.JPEG, 50, outputStreamInternal)
            val internalBytes = outputStreamInternal.toByteArray()
            val internalBase64 = Base64.encodeToString(internalBytes, Base64.NO_WRAP)
            
            val rawSignal = aiClient.analyzeInternalCamera(internalBase64)
            val validationResult = confidenceValidator.validate(rawSignal.confidence)
            
            if (validationResult.isValid) {
                rawSignal
            } else {
                rawSignal.copy(
                    mood = "uncertain (low confidence: ${validationResult.formattedConfidence})",
                    confidence = rawSignal.confidence
                )
            }
        } else {
            InternalCameraSignal(
                mood = "unknown",
                confidence = 0.0,
                passengers = PassengersDetail(0, 0, 0)
            )
        }

        // Weather
        val weatherResult = weatherService.getCurrentWeather()

        val signals = Signals(
            vehicle = VehicleSignal(speed_kmh = location?.speed?.times(3.6) ?: 0.0),
            environment = EnvironmentSignal(
                time_of_day = getHourOfDay(),
                weather = weatherResult.weather,
                temperature = weatherResult.temperature,
                date_type = "weekday"
            ),
            external_camera = finalExternalSignal,
            internal_camera = internalSignal,
            internal_mic = InternalMicSignal(
                volume_level = micData.volume,
                has_voice = micData.hasVoice,
                voice_count = micData.voiceCount,
                noise_level = micData.noiseLevel
            )
        )

        val output = StandardizedSignals(
            timestamp = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date()),
            signals = signals,
            confidence = Confidence(overall = 0.9)
        )

        _signalsFlow.emit(output)
    }

    override fun stop() {
        isRunning = false
        loopJob?.cancel()
        sensorManager.stopAudio()
        cameraSource.stop()
        ipCameraSource.stop()
    }

    override fun destroy() {
        stop()
        scope.cancel()
    }

    override fun updateConfig(config: Layer1Config) {
        this.config = config
        this.aiClient = AIClient(config) // Update AI Client with new config
        
        ipCameraSource.stop()
        ipCameraSource = IpCameraSource(config)
        setupIpCameraCallback()
        if (isRunning) ipCameraSource.start()
    }

    private fun getHourOfDay(): Double {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)
        return hour + (minute / 60.0)
    }
}
