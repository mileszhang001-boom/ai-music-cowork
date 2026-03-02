package com.music.localmusic.source

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbManager
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed class UsbEvent {
    data class DeviceAttached(val deviceName: String) : UsbEvent()
    data class DeviceDetached(val deviceName: String) : UsbEvent()
}

class UsbStateMonitor(private val context: Context) {
    
    companion object {
        private const val TAG = "UsbStateMonitor"
    }
    
    private val _usbEventFlow = MutableStateFlow<UsbEvent?>(null)
    val usbEventFlow: StateFlow<UsbEvent?> = _usbEventFlow.asStateFlow()
    
    private val _isUsbConnected = MutableStateFlow(false)
    val isUsbConnected: StateFlow<Boolean> = _isUsbConnected.asStateFlow()
    
    private val _connectedDevices = MutableStateFlow<List<String>>(emptyList())
    val connectedDevices: StateFlow<List<String>> = _connectedDevices.asStateFlow()
    
    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                UsbManager.ACTION_USB_DEVICE_ATTACHED -> {
                    val device = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, android.hardware.usb.UsbDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
                    }
                    
                    val deviceName = device?.deviceName ?: "Unknown"
                    Log.i(TAG, "USB 设备已连接: $deviceName")
                    
                    _usbEventFlow.value = UsbEvent.DeviceAttached(deviceName)
                    _isUsbConnected.value = true
                    updateConnectedDevices()
                }
                
                UsbManager.ACTION_USB_DEVICE_DETACHED -> {
                    val device = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, android.hardware.usb.UsbDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
                    }
                    
                    val deviceName = device?.deviceName ?: "Unknown"
                    Log.i(TAG, "USB 设备已断开: $deviceName")
                    
                    _usbEventFlow.value = UsbEvent.DeviceDetached(deviceName)
                    updateConnectedDevices()
                    
                    val usbManager = context.getSystemService(Context.USB_SERVICE) as? UsbManager
                    _isUsbConnected.value = usbManager?.deviceList?.isNotEmpty() ?: false
                }
            }
        }
    }
    
    fun register() {
        val filter = IntentFilter().apply {
            addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
            addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
        }
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(broadcastReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(broadcastReceiver, filter)
        }
        
        updateConnectedDevices()
        Log.i(TAG, "USB 监听器已注册")
    }
    
    fun unregister() {
        try {
            context.unregisterReceiver(broadcastReceiver)
            Log.i(TAG, "USB 监听器已注销")
        } catch (e: Exception) {
            Log.w(TAG, "注销 USB 监听器失败", e)
        }
    }
    
    private fun updateConnectedDevices() {
        val usbManager = context.getSystemService(Context.USB_SERVICE) as? UsbManager
        val devices = usbManager?.deviceList?.keys?.toList() ?: emptyList()
        _connectedDevices.value = devices
        _isUsbConnected.value = devices.isNotEmpty()
    }
    
    fun getUsbDevices(): List<android.hardware.usb.UsbDevice> {
        val usbManager = context.getSystemService(Context.USB_SERVICE) as? UsbManager
        return usbManager?.deviceList?.values?.toList() ?: emptyList()
    }
    
    fun clearEvent() {
        _usbEventFlow.value = null
    }
}
