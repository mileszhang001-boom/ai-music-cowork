# Android Vehicle Interface Specification (AI-Ready)

**Description**: This document defines the interface specifications for accessing vehicle hardware and system services in an Android Automotive environment. It is optimized for AI coding assistants to generate correct implementation code.

## 1. System Context

*   **Platform**: Android / Android Automotive OS (AAOS)
*   **Language**: Kotlin (Preferred) / Java
*   **Core Mechanism**: **Java Reflection** is STRICTLY REQUIRED for `android.car` APIs to ensure compatibility across different vendor implementations and avoid compile-time dependency issues.
*   **Error Handling**: All hardware access must be wrapped in `try-catch` blocks.
*   **Threading**: UI updates must be posted to the Main Thread.

---

## 2. Module: Car Control (Atmosphere Light)

**Target Class**: `android.car.hardware.property.CarPropertyManager` (accessed via Reflection)

### 2.1 Initialization Strategy

**Algorithm**:
1.  Reflectively load `android.car.Car`.
2.  Reflectively invoke `createCar` with a `ServiceConnection` or `BiConsumer` callback.
3.  On connection success, reflectively invoke `getCarManager("property")` to obtain the manager instance.

**Code Template (Kotlin)**:
```kotlin
// Context: Activity or Fragment
val carClass = Class.forName("android.car.Car")
val createCar = carClass.getMethod("createCar", Context::class.java, Handler::class.java, Int::class.java, java.util.function.BiConsumer::class.java)
createCar.invoke(null, context, Handler(), 0, java.util.function.BiConsumer { car: Any, ready: Boolean ->
    if (ready) {
        val getManager = car.javaClass.getMethod("getCarManager", String::class.java)
        val manager = getManager.invoke(car, "property") // "property" constant
        // Store manager for later use
    }
})
```

### 2.2 Property Definitions (Atmosphere Light)

| Property Name | Property ID (Hex) | Data Type | Access | Description |
| :--- | :--- | :--- | :--- | :--- |
| `LIGHT_SWITCH` | `0x61402504` | `Boolean` | R/W | Master switch ON/OFF |
| `LIGHT_COLOR` | `0x61402513` | `Int` | R/W | RGB Color Value |
| `LIGHT_BRIGHTNESS` | `0x6140250c` | `Float` | R/W | Range: 0.0 (min) - 1.0 (max) |
| `LIGHT_MODE` | `0x61402509` | `Int` | R/W | 0:Static, 1:Breathing, 2:Gradient, 3:Flash, 4:Music |
| `LIGHT_SPEED` | `0x61402511` | `Int` | R/W | Speed level: 1 - 10 |

### 2.3 Command Execution (Set Property)

**Requirement**: Implement a retry mechanism that attempts multiple `setProperty` method signatures due to API fragmentation.

**Signature Priority List**:
1.  `setProperty(Class<T>, int propId, int areaId, T value)`
2.  `setProperty(Class<T>, int propId, int areaId, T value, int flags)`
3.  `setProperty(Class<T>, int propId, T value)` (Implied areaId=0)

**Required Permissions**:
*   `android.car.permission.CAR_LIGHTING`

---

## 3. Module: Sensors & Peripherals

### 3.1 Camera System

**API**: `android.hardware.camera2`

**Functional Requirements**:
*   **Discovery**: Iterate `CameraManager.getCameraIdList()`.
*   **Preview**: Use `TextureView` as the preview surface.
*   **Configuration**:
    *   Target Usage: `TEMPLATE_PREVIEW`
    *   Focus Mode: `CONTROL_AF_MODE_CONTINUOUS_PICTURE`
    *   Stabilization: `CONTROL_VIDEO_STABILIZATION_MODE_ON` (if supported)

**Required Permissions**:
*   `android.permission.CAMERA`

### 3.2 Location Services

**API**: `android.location.LocationManager`

**Functional Requirements**:
*   **Provider Selection**: Priority `GPS_PROVIDER` > `NETWORK_PROVIDER`.
*   **Data Structure**:
    ```json
    {
      "latitude": "Double",
      "longitude": "Double",
      "altitude": "Double (Optional)",
      "address": "String (Reverse Geocoded)"
    }
    ```
*   **Reverse Geocoding**: Use `android.location.Geocoder`.

**Required Permissions**:
*   `android.permission.ACCESS_FINE_LOCATION`
*   `android.permission.ACCESS_COARSE_LOCATION`

### 3.3 Microphone (Audio Input)

**API**: `android.media.AudioRecord`

**Configuration Constants**:
*   `SAMPLE_RATE`: 44100
*   `CHANNEL_CONFIG`: `AudioFormat.CHANNEL_IN_STEREO`
*   `AUDIO_FORMAT`: `AudioFormat.ENCODING_PCM_16BIT`
*   `SOURCE`: `MediaRecorder.AudioSource.MIC`

**Output**: Raw PCM data (convert to WAV for playback compatibility).

**Required Permissions**:
*   `android.permission.RECORD_AUDIO`

---

## 4. Module: System Services

### 4.1 Text-To-Speech (TTS)

**API**: `android.speech.tts.TextToSpeech`

**Initialization Flow**:
1.  Construct `TextToSpeech(context, listener)`.
2.  In `onInit(status)`:
    *   Check `status == SUCCESS`.
    *   Set Language: `result = setLanguage(Locale.CHINESE)`.
    *   Check `result != LANG_MISSING_DATA && result != LANG_NOT_SUPPORTED`.

### 4.2 External Weather Data

**Protocol**: HTTP GET
**Providers**: Open-Meteo, QWeather
**Data Parsing**: JSON

**Required Permissions**:
*   `android.permission.INTERNET`

---

## 5. Implementation Checklist for AI

When generating code based on this spec, the AI MUST:
1.  [ ] Check for Permissions at runtime before accessing hardware.
2.  [ ] Wrap all Reflection calls in `try-catch(Exception)`.
3.  [ ] Ensure UI updates happen on `runOnUiThread`.
4.  [ ] Release resources (Camera, AudioRecord, TTS) in `onDestroy` or `onPause`.
5.  [ ] Add Chinese comments (Function-level) as per user preference.
