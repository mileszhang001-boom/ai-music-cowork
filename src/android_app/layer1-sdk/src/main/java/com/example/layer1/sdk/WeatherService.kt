package com.example.layer1.sdk

import com.example.layer1.api.data.EnvironmentSignal
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

class WeatherService {

    private val client = OkHttpClient()
    private val gson = Gson()

    private val LATITUDE = 39.9042
    private val LONGITUDE = 116.4074

    data class WeatherResult(
        val weather: String,
        val temperature: Double
    )

    private data class OpenMeteoResponse(
        val current_weather: CurrentWeather
    )

    private data class CurrentWeather(
        val temperature: Double,
        val weathercode: Int
    )

    suspend fun getCurrentWeather(): WeatherResult = withContext(Dispatchers.IO) {
        val url = "https://api.open-meteo.com/v1/forecast?latitude=$LATITUDE&longitude=$LONGITUDE&current_weather=true"
        
        try {
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            
            if (response.isSuccessful) {
                val body = response.body?.string()
                if (body != null) {
                    val data = gson.fromJson(body, OpenMeteoResponse::class.java)
                    val weatherCode = data.current_weather.weathercode
                    val weatherDesc = mapWeatherCode(weatherCode)
                    return@withContext WeatherResult(weatherDesc, data.current_weather.temperature)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        return@withContext WeatherResult("unknown", 22.0)
    }

    private fun mapWeatherCode(code: Int): String {
        return when (code) {
            0 -> "clear"
            1, 2, 3 -> "cloudy"
            45, 48 -> "foggy"
            51, 53, 55, 56, 57 -> "drizzle"
            61, 63, 65, 66, 67 -> "rainy"
            71, 73, 75, 77 -> "snowy"
            80, 81, 82 -> "heavy_rain"
            85, 86 -> "snow_showers"
            95, 96, 99 -> "thunderstorm"
            else -> "unknown"
        }
    }
}
