package com.music.perception.sdk

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import com.music.perception.api.PerceptionConfig
import okhttp3.Credentials
import java.io.BufferedReader
import java.io.DataInputStream
import java.io.InputStreamReader
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
                    connection.readTimeout = 30000
                    connection.doInput = true
                    connection.connect()
                    
                    val responseCode = connection.responseCode
                    Log.d("IpCameraSource", "Connection response: $responseCode")
                    
                    if (responseCode != 200) {
                        Log.e("IpCameraSource", "Failed to connect: $responseCode")
                        connection.disconnect()
                        Thread.sleep(2000)
                        continue
                    }
                    
                    val inputStream = DataInputStream(connection.inputStream)
                    
                    val buffer = ByteArray(1024 * 1024)
                    var bufferIdx = 0
                    var findingStart = true
                    
                    Log.d("IpCameraSource", "Starting to read MJPEG stream...")
                    
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
                            
                            if (bufferIdx >= 2 && 
                                buffer[bufferIdx - 2] == 0xFF.toByte() && 
                                buffer[bufferIdx - 1] == 0xD9.toByte()) {
                                
                                val frameData = buffer.copyOfRange(0, bufferIdx)
                                val bitmap = BitmapFactory.decodeByteArray(frameData, 0, frameData.size)
                                
                                if (bitmap != null) {
                                    Log.d("IpCameraSource", "Frame decoded: ${bitmap.width}x${bitmap.height}")
                                    callback?.onFrameAvailable(bitmap)
                                } else {
                                    Log.w("IpCameraSource", "Failed to decode frame, size: ${frameData.size}")
                                }
                                
                                findingStart = true
                                bufferIdx = 0
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
                    Log.e("IpCameraSource", "Stream error: ${e.message}", e)
                    try {
                        Thread.sleep(2000)
                    } catch (ie: InterruptedException) {
                        break
                    }
                }
            }
            Log.d("IpCameraSource", "MJPEG stream stopped")
        }
        thread?.start()
    }

    fun stop() {
        Log.d("IpCameraSource", "Stopping MJPEG stream...")
        isRunning.set(false)
        thread?.interrupt()
        thread = null
    }
}
