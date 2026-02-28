package com.music.semantic.llm

import com.music.core.api.models.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

@Serializable
data class LlmMessage(
    val role: String,
    val content: String
)

@Serializable
data class LlmRequest(
    val model: String,
    val messages: List<LlmMessage>,
    val temperature: Double = 0.7,
    val max_tokens: Int = 2000
)

@Serializable
data class LlmResponse(
    val id: String? = null,
    val choices: List<LlmChoice>? = null,
    val usage: LlmUsage? = null
)

@Serializable
data class LlmChoice(
    val index: Int = 0,
    val message: LlmMessage? = null,
    val finish_reason: String? = null
)

@Serializable
data class LlmUsage(
    val prompt_tokens: Int = 0,
    val completion_tokens: Int = 0,
    val total_tokens: Int = 0
)

class LlmClient(
    private val apiKey: String,
    private val baseUrl: String = "https://dashscope.aliyuncs.com/compatible-mode/v1",
    private val model: String = "qwen-plus"
) {
    private val json = Json { 
        ignoreUnknownKeys = true 
        isLenient = true
    }
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    
    suspend fun chat(messages: List<LlmMessage>): Result<String> = withContext(Dispatchers.IO) {
        try {
            val request = LlmRequest(
                model = model,
                messages = messages,
                temperature = 0.7,
                max_tokens = 2000
            )
            
            val requestBody = json.encodeToString(LlmRequest.serializer(), request)
                .toRequestBody("application/json".toMediaType())
            
            val httpRequest = Request.Builder()
                .url("$baseUrl/chat/completions")
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .post(requestBody)
                .build()
            
            val response = client.newCall(httpRequest).execute()
            
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("LLM API Error: ${response.code}"))
            }
            
            val responseBody = response.body?.string() 
                ?: return@withContext Result.failure(Exception("Empty response body"))
            
            val llmResponse = json.decodeFromString<LlmResponse>(responseBody)
            val content = llmResponse.choices?.firstOrNull()?.message?.content
                ?: return@withContext Result.failure(Exception("No content in response"))
            
            Result.success(content)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    fun isReady(): Boolean = apiKey.isNotBlank()
}
