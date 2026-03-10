package com.music.perception.test.mock.serializer

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonSyntaxException
import com.music.perception.api.StandardizedSignals

/**
 * 数据序列化工具类
 * 提供数据的序列化和反序列化功能
 */
object DataSerializer {
    
    private val gson: Gson = GsonBuilder()
        .setPrettyPrinting()
        .serializeNulls()
        .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
        .create()
    
    /**
     * 序列化对象为 JSON 字符串
     * @param data 待序列化的对象
     * @return JSON 字符串
     */
    fun <T> toJson(data: T): String {
        return gson.toJson(data)
    }
    
    /**
     * 反序列化 JSON 字符串为对象
     * @param json JSON 字符串
     * @param clazz 目标类
     * @return 反序列化的对象
     */
    fun <T> fromJson(json: String, clazz: Class<T>): T {
        return gson.fromJson(json, clazz)
    }
    
    /**
     * 反序列化 JSON 字符串为 StandardizedSignals
     * @param json JSON 字符串
     * @return StandardizedSignals 对象，失败返回 null
     */
    fun fromJsonToStandardizedSignals(json: String): StandardizedSignals? {
        return try {
            gson.fromJson(json, StandardizedSignals::class.java)
        } catch (e: JsonSyntaxException) {
            null
        }
    }
    
    /**
     * 验证 JSON 格式是否正确
     * @param json JSON 字符串
     * @return 是否有效
     */
    fun isValidJson(json: String): Boolean {
        return try {
            gson.fromJson(json, Any::class.java)
            true
        } catch (e: JsonSyntaxException) {
            false
        }
    }
    
    /**
     * 格式化 JSON 字符串
     * @param json JSON 字符串
     * @return 格式化后的 JSON 字符串
     */
    fun formatJson(json: String): String {
        return try {
            val element = gson.fromJson(json, Any::class.java)
            gson.toJson(element)
        } catch (e: JsonSyntaxException) {
            json
        }
    }
    
    /**
     * 压缩 JSON 字符串（移除空格和换行）
     * @param json JSON 字符串
     * @return 压缩后的 JSON 字符串
     */
    fun compressJson(json: String): String {
        return json.replace("\\s+".toRegex(), "")
    }
    
    /**
     * 计算 JSON 数据大小（字节）
     * @param json JSON 字符串
     * @return 字节数
     */
    fun calculateJsonSize(json: String): Int {
        return json.toByteArray(Charsets.UTF_8).size
    }
    
    /**
     * 批量序列化对象列表
     * @param dataList 对象列表
     * @return JSON 字符串列表
     */
    fun <T> toJsonList(dataList: List<T>): List<String> {
        return dataList.map { toJson(it) }
    }
    
    /**
     * 批量反序列化 JSON 字符串列表
     * @param jsonList JSON 字符串列表
     * @param clazz 目标类
     * @return 对象列表
     */
    fun <T> fromJsonList(jsonList: List<String>, clazz: Class<T>): List<T> {
        return jsonList.map { fromJson(it, clazz) }
    }
}
