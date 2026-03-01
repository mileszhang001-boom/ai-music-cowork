package com.example.layer1.api

import com.example.layer1.api.data.StandardizedSignals

interface ILayer1EventListener {
    fun onSignalUpdate(signal: StandardizedSignals)
    fun onError(error: Layer1Error)
}
