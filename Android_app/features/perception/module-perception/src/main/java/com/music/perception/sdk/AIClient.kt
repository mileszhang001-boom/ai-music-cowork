package com.music.perception.sdk

import android.util.Log
import com.music.perception.api.PerceptionConfig
import com.music.core.api.models.ExternalCamera
import com.music.core.api.models.InternalCamera
import com.music.core.api.models.Passengers
import com.google.gson.Gson
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class AIClient(private val config: PerceptionConfig) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()
    private val baseUrl = "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions"

    fun analyzeExternalCamera(base64Image: String): ExternalCamera {
        try {
            val jsonBody = JSONObject()
            jsonBody.put("model", "qwen-vl-max")
            
            val messages = org.json.JSONArray()
            val userMessage = JSONObject()
            userMessage.put("role", "user")
            
            val contentArray = org.json.JSONArray()
            
            val textContent = JSONObject()
            textContent.put("type", "text")
            textContent.put("text", "请分析这张车外环境图片。请提供简短的场景描述（scene_description），例如：'city street at night', 'highway in rain', 'sunny rural road'。同时提取环境主色（primary_color）和亮度（brightness: 0.0-1.0）。请仅返回JSON格式：{\"scene_description\": \"...\", \"primary_color\": \"#...\", \"brightness\": ...}")
            contentArray.put(textContent)
            
            val imageContent = JSONObject()
            imageContent.put("type", "image_url")
            val imageUrl = JSONObject()
            imageUrl.put("url", "data:image/jpeg;base64,$base64Image")
            imageContent.put("image_url", imageUrl)
            contentArray.put(imageContent)
            
            userMessage.put("content", contentArray)
            messages.put(userMessage)
            
            jsonBody.put("messages", messages)

            val request = Request.Builder()
                .url(baseUrl)
                .addHeader("Authorization", "Bearer ${config.dashScopeApiKey}")
                .addHeader("Content-Type", "application/json")
                .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()
            
            if (response.isSuccessful && responseBody != null) {
                val respJson = JSONObject(responseBody)
                val content = respJson.getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content")
                
                val jsonStart = content.indexOf("{")
                val jsonEnd = content.lastIndexOf("}")
                if (jsonStart != -1 && jsonEnd != -1) {
                    val cleanJson = content.substring(jsonStart, jsonEnd + 1)
                    val parsed = gson.fromJson(cleanJson, ExternalCameraResponse::class.java)
                    return ExternalCamera(
                        primary_color = parsed.primary_color,
                        secondary_color = null,
                        brightness = parsed.brightness,
                        scene_description = parsed.scene_description
                    )
                }
            } else {
                Log.e("AIClient", "API Error: ${response.code} $responseBody")
            }
        } catch (e: Exception) {
            Log.e("AIClient", "Exception: ${e.message}")
        }

        return getMockExternalCamera()
    }

    private data class ExternalCameraResponse(
        val scene_description: String?,
        val primary_color: String?,
        val brightness: Double?
    )

    private fun getMockExternalCamera(): ExternalCamera {
        return ExternalCamera(
            scene_description = "city_driving_mock",
            primary_color = "#808080",
            secondary_color = null,
            brightness = 0.5
        )
    }

    fun analyzeInternalCamera(base64Image: String): InternalCamera {
        try {
            val jsonBody = JSONObject()
            jsonBody.put("model", "qwen-vl-max")
            
            val messages = org.json.JSONArray()
            val userMessage = JSONObject()
            userMessage.put("role", "user")
            
            val contentArray = org.json.JSONArray()
            
            val textContent = JSONObject()
            textContent.put("type", "text")
            textContent.put("text", "请分析这张车内监控图片。重点识别：1. 主驾驶员的情绪状态（mood: happy, angry, tired, stressed, focused, neutral）。2. 车内乘客数量和类型（passengers: children, adults, seniors）。请仅返回JSON格式：{\"mood\": \"...\", \"confidence\": 0.9, \"passengers\": {\"adults\": ..., \"children\": ..., \"seniors\": ...}}")
            contentArray.put(textContent)
            
            val imageContent = JSONObject()
            imageContent.put("type", "image_url")
            val imageUrl = JSONObject()
            imageUrl.put("url", "data:image/jpeg;base64,$base64Image")
            imageContent.put("image_url", imageUrl)
            contentArray.put(imageContent)
            
            userMessage.put("content", contentArray)
            messages.put(userMessage)
            
            jsonBody.put("messages", messages)

            val request = Request.Builder()
                .url(baseUrl)
                .addHeader("Authorization", "Bearer ${config.dashScopeApiKey}")
                .addHeader("Content-Type", "application/json")
                .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()
            
            if (response.isSuccessful && responseBody != null) {
                val respJson = JSONObject(responseBody)
                val content = respJson.getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content")
                
                val jsonStart = content.indexOf("{")
                val jsonEnd = content.lastIndexOf("}")
                if (jsonStart != -1 && jsonEnd != -1) {
                    val cleanJson = content.substring(jsonStart, jsonEnd + 1)
                    val parsed = gson.fromJson(cleanJson, InternalCameraResponse::class.java)
                    return InternalCamera(
                        mood = parsed.mood,
                        confidence = parsed.confidence,
                        passengers = Passengers(
                            children = parsed.passengers?.children ?: 0,
                            adults = parsed.passengers?.adults ?: 1,
                            seniors = parsed.passengers?.seniors ?: 0
                        )
                    )
                }
            } else {
                Log.e("AIClient", "API Error: ${response.code} $responseBody")
            }
        } catch (e: Exception) {
            Log.e("AIClient", "Exception: ${e.message}")
        }

        return getMockInternalCamera()
    }

    private data class InternalCameraResponse(
        val mood: String?,
        val confidence: Double?,
        val passengers: PassengersResponse?
    )

    private data class PassengersResponse(
        val children: Int?,
        val adults: Int?,
        val seniors: Int?
    )

    private fun getMockInternalCamera(): InternalCamera {
        return InternalCamera(
            mood = "neutral",
            confidence = 0.85,
            passengers = Passengers(adults = 1)
        )
    }
}
