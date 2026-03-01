package com.music.perception.sdk

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import com.music.perception.api.PerceptionConfig
import okhttp3.Credentials
import java.io.DataInputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.atomic.AtomicBoolean

class IpCameraSource(private val config: PerceptionConfig) {

    private var isRunning = AtomicBoolean(false)
    private var thread: Thread? = null
    
    interface FrameCallback {
        fun onFrameAvailable(bitmap: Bitmap)
    }
    
    private var callback: FrameCallback? = null
    
    fun setCallback(cb: FrameCallback) {
        this.callback = cb
    }

    fun start() {
        if (isRunning.get()) return
        isRunning.set(true)
        
        thread = Thread {
            Log.d("IpCameraSource", "Starting MJPEG stream from ${config.ipCameraUrl}")
            while (isRunning.get()) {
                try {
                    val url = URL(config.ipCameraUrl)
                    val connection = url.openConnection() as HttpURLConnection
                    val auth = Credentials.basic(config.ipCameraUsername, config.ipCameraPassword)
                    connection.setRequestProperty("Authorization", auth)
                    connection.connectTimeout = 5000
                    connection.readTimeout = 10000
                    connection.doInput = true
                    connection.connect()

                    val inputStream = DataInputStream(connection.inputStream)
                    
                    val buffer = ByteArray(1024 * 1024)
                    var bufferIdx = 0
                    var findingStart = true
                    
                    while (isRunning.get()) {
                        val byte = inputStream.readUnsignedByte()
                        
                        if (findingStart) {
                            if (byte == 0xFF) {
                                val next = inputStream.readUnsignedByte()
                                if (next == 0xD8) {
                                    findingStart = false
                                    bufferIdx = 0
                                    buffer[bufferIdx++] = 0xFF.toByte()
                                    buffer[bufferIdx++] = 0xD8.toByte()
                                }
                            }
                        } else {
                            buffer[bufferIdx++] = byte.toByte()
                            if (byte == 0xD9) {
                                if (buffer[bufferIdx - 2] == 0xFF.toByte()) {
                                    val frameData = buffer.copyOfRange(0, bufferIdx)
                                    val bitmap = BitmapFactory.decodeByteArray(frameData, 0, frameData.size)
                                    if (bitmap != null) {
                                        callback?.onFrameAvailable(bitmap)
                                    }
                                    findingStart = true
                                    bufferIdx = 0
                                }
                            }
                            
                            if (bufferIdx >= buffer.size) {
                                findingStart = true
                                bufferIdx = 0
                                Log.w("IpCameraSource", "Frame buffer overflow, resetting")
                            }
                        }
                    }
                    inputStream.close()
                    connection.disconnect()
                    
                } catch (e: Exception) {
                    Log.e("IpCameraSource", "Stream error: ${e.message}")
                    try {
                        Thread.sleep(2000)
                    } catch (ie: InterruptedException) {
                        break
                    }
                }
            }
        }
        thread?.start()
    }

    fun stop() {
        isRunning.set(false)
        thread?.interrupt()
        thread = null
    }
}
