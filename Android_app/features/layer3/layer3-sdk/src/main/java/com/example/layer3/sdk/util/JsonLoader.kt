package com.example.layer3.sdk.util

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.BufferedReader

object JsonLoader {
    val gson = Gson()

    fun loadJsonFromAssets(context: Context, fileName: String): String? {
        return try {
            val inputStream = context.assets.open(fileName)
            val bufferedReader = BufferedReader(inputStream.reader())
            val content = bufferedReader.use { it.readText() }
            inputStream.close()
            content
        } catch (e: Exception) {
            null
        }
    }

    inline fun <reified T> loadFromAssets(context: Context, fileName: String): T? {
        val json = loadJsonFromAssets(context, fileName) ?: return null
        return try {
            gson.fromJson(json, object : TypeToken<T>() {}.type)
        } catch (e: Exception) {
            null
        }
    }

    fun <T> fromJson(json: String, clazz: Class<T>): T? {
        return try {
            gson.fromJson(json, clazz)
        } catch (e: Exception) {
            null
        }
    }

    fun toJson(obj: Any): String {
        return gson.toJson(obj)
    }
}
