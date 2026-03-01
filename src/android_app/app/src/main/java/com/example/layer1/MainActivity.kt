package com.example.layer1

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import com.example.layer1.api.Layer1Config
import com.example.layer1.sdk.Layer1SDK
import com.google.gson.GsonBuilder
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var tvOutput: TextView
    private lateinit var tvStatus: TextView
    private val gson = GsonBuilder().setPrettyPrinting().create()
    private var isRunning = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvOutput = findViewById(R.id.tv_output)
        tvStatus = findViewById(R.id.tv_status)

        // Initialize SDK
        val config = Layer1Config.Builder()
            .setIpCameraUrl("http://172.31.2.50:8081/video")
            .setIpCameraAuth("admin", "123456")
            .setDashScopeApiKey("sk-fb1a1b32bf914059a043ee4ebd1c845a")
            .setRefreshInterval(3000L)
            .build()
        
        Layer1SDK.init(applicationContext, config, this)

        findViewById<Button>(R.id.btn_start).setOnClickListener {
            if (!checkPermissions()) {
                requestPermissions()
            } else {
                togglePerception()
            }
        }
        
        // Subscribe to signals
        lifecycleScope.launch {
            Layer1SDK.getEngine().standardizedSignalsFlow.collect { signals ->
                tvOutput.text = gson.toJson(signals)
            }
        }
    }

    private fun checkPermissions(): Boolean {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED &&
               ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
               ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.INTERNET
            ),
            100
        )
    }

    private fun togglePerception() {
        isRunning = !isRunning
        if (isRunning) {
            tvStatus.text = "Status: Running..."
            Layer1SDK.getEngine().start()
        } else {
            tvStatus.text = "Status: Stopped"
            Layer1SDK.getEngine().stop()
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        Layer1SDK.destroy()
    }
}
