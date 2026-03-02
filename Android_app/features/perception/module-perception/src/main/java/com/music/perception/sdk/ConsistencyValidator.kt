package com.music.perception.sdk

import com.music.core.api.models.StandardizedSignals
import com.music.core.api.models.InternalCamera
import com.music.core.api.models.InternalMic
import com.music.core.api.models.Passengers
import com.music.core.api.models.ExternalCamera
import com.music.core.api.models.Vehicle
import kotlinx.serialization.Serializable
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.math.abs

/**
 * 一致性校验结果
 */
@Serializable
data class ConsistencyResult(
    val isConsistent: Boolean,
    val timestamp: Long,
    val previousTimestamp: Long? = null,
    val consistencyScore: Double = 0.0,
    val matchingFields: List<String> = emptyList(),
    val differingFields: List<String> = emptyList(),
    val dataHash: String = "",
    val sampleCount: Int = 1,
    val waitTimeMs: Long = 0
)

/**
 * 缓存的感知数据样本
 */
data class CachedSample(
    val data: StandardizedSignals,
    val timestamp: Long,
    val hash: String
)

/**
 * 一致性校验器 - 方案A：相似度阈值 + 关键字段匹配
 * 
 * 功能：
 * 1. 缓存每次采集的原始数据并记录时间戳
 * 2. 比较连续两次结果的关键字段
 * 3. 支持相似度阈值（置信度允许5%误差）
 * 4. 线程安全设计
 */
class ConsistencyValidator(
    private val confidenceTolerance: Double = 0.05,
    private val maxWaitSamples: Int = 5,
    private val timeoutMs: Long = 30000L
) {
    private val lock = ReentrantLock()
    private val cachedSample = AtomicReference<CachedSample?>(null)
    private var sampleCount = 0
    private var firstSampleTime: Long = 0
    
    /**
     * 校验新数据与缓存数据的一致性
     * @param newData 新采集的标准化数据
     * @return 一致性校验结果
     */
    fun validate(newData: StandardizedSignals): ConsistencyResult {
        lock.withLock {
            val currentTimestamp = System.currentTimeMillis()
            val newHash = computeHash(newData)
            val previous = cachedSample.get()
            
            if (previous == null) {
                val sample = CachedSample(newData, currentTimestamp, newHash)
                cachedSample.set(sample)
                sampleCount = 1
                firstSampleTime = currentTimestamp
                
                return ConsistencyResult(
                    isConsistent = false,
                    timestamp = currentTimestamp,
                    consistencyScore = 0.0,
                    matchingFields = emptyList(),
                    differingFields = listOf("首次采集，无对比数据"),
                    dataHash = newHash,
                    sampleCount = 1,
                    waitTimeMs = 0
                )
            }
            
            sampleCount++
            val waitTime = currentTimestamp - firstSampleTime
            
            val comparison = compareData(previous.data, newData)
            val isConsistent = comparison.isSimilar
            
            if (isConsistent) {
                val result = ConsistencyResult(
                    isConsistent = true,
                    timestamp = currentTimestamp,
                    previousTimestamp = previous.timestamp,
                    consistencyScore = comparison.score,
                    matchingFields = comparison.matchingFields,
                    differingFields = comparison.differingFields,
                    dataHash = newHash,
                    sampleCount = sampleCount,
                    waitTimeMs = waitTime
                )
                
                cachedSample.set(CachedSample(newData, currentTimestamp, newHash))
                sampleCount = 1
                firstSampleTime = currentTimestamp
                
                return result
            }
            
            if (shouldTimeout(currentTimestamp)) {
                val result = ConsistencyResult(
                    isConsistent = false,
                    timestamp = currentTimestamp,
                    previousTimestamp = previous.timestamp,
                    consistencyScore = comparison.score,
                    matchingFields = comparison.matchingFields,
                    differingFields = comparison.differingFields,
                    dataHash = newHash,
                    sampleCount = sampleCount,
                    waitTimeMs = waitTime
                )
                
                cachedSample.set(CachedSample(newData, currentTimestamp, newHash))
                sampleCount = 1
                firstSampleTime = currentTimestamp
                
                return result
            }
            
            cachedSample.set(CachedSample(newData, currentTimestamp, newHash))
            
            return ConsistencyResult(
                isConsistent = false,
                timestamp = currentTimestamp,
                previousTimestamp = previous.timestamp,
                consistencyScore = comparison.score,
                matchingFields = comparison.matchingFields,
                differingFields = comparison.differingFields,
                dataHash = newHash,
                sampleCount = sampleCount,
                waitTimeMs = waitTime
            )
        }
    }
    
    /**
     * 比较两个数据的相似度
     */
    private fun compareData(old: StandardizedSignals, new: StandardizedSignals): ComparisonResult {
        val matchingFields = mutableListOf<String>()
        val differingFields = mutableListOf<String>()
        var totalScore = 0.0
        var fieldCount = 0
        
        val oldInternal = old.signals?.internal_camera
        val newInternal = new.signals?.internal_camera
        
        if (oldInternal != null && newInternal != null) {
            val internalResult = compareInternalCamera(oldInternal, newInternal)
            matchingFields.addAll(internalResult.matching)
            differingFields.addAll(internalResult.differing)
            totalScore += internalResult.score
            fieldCount++
        } else if (oldInternal != newInternal) {
            differingFields.add("internal_camera: 一方为空")
            fieldCount++
        }
        
        val oldMic = old.signals?.internal_mic
        val newMic = new.signals?.internal_mic
        
        if (oldMic != null && newMic != null) {
            val micResult = compareInternalMic(oldMic, newMic)
            matchingFields.addAll(micResult.matching)
            differingFields.addAll(micResult.differing)
            totalScore += micResult.score
            fieldCount++
        } else if (oldMic != newMic) {
            differingFields.add("internal_mic: 一方为空")
            fieldCount++
        }
        
        val oldExternal = old.signals?.external_camera
        val newExternal = new.signals?.external_camera
        
        if (oldExternal != null && newExternal != null) {
            val externalResult = compareExternalCamera(oldExternal, newExternal)
            matchingFields.addAll(externalResult.matching)
            differingFields.addAll(externalResult.differing)
            totalScore += externalResult.score
            fieldCount++
        }
        
        val oldVehicle = old.signals?.vehicle
        val newVehicle = new.signals?.vehicle
        
        if (oldVehicle != null && newVehicle != null) {
            val vehicleResult = compareVehicle(oldVehicle, newVehicle)
            matchingFields.addAll(vehicleResult.matching)
            differingFields.addAll(vehicleResult.differing)
            totalScore += vehicleResult.score
            fieldCount++
        }
        
        if (old.confidence != null && new.confidence != null) {
            val overallDiff = abs(old.confidence.overall - new.confidence.overall)
            if (overallDiff <= confidenceTolerance) {
                matchingFields.add("confidence.overall (误差: ${"%.3f".format(overallDiff)})")
                totalScore += 1.0
            } else {
                differingFields.add("confidence.overall: ${old.confidence.overall} -> ${new.confidence.overall}")
            }
            fieldCount++
        }
        
        val avgScore = if (fieldCount > 0) totalScore / fieldCount else 0.0
        val isSimilar = differingFields.isEmpty() || (differingFields.size <= 1 && avgScore >= 0.8)
        
        return ComparisonResult(
            isSimilar = isSimilar,
            score = avgScore,
            matchingFields = matchingFields,
            differingFields = differingFields
        )
    }
    
    /**
     * 比较内部摄像头数据
     */
    private fun compareInternalCamera(old: InternalCamera, new: InternalCamera): FieldComparisonResult {
        val matching = mutableListOf<String>()
        val differing = mutableListOf<String>()
        var score = 0.0
        
        if (old.mood == new.mood) {
            matching.add("internal_camera.mood: ${old.mood}")
            score += 1.0
        } else {
            differing.add("internal_camera.mood: ${old.mood} -> ${new.mood}")
        }
        
        val oldConfidence = old.confidence
        val newConfidence = new.confidence
        if (oldConfidence != null && newConfidence != null) {
            val diff = abs(oldConfidence - newConfidence)
            if (diff <= confidenceTolerance) {
                matching.add("internal_camera.confidence (误差: ${"%.3f".format(diff)})")
                score += 1.0
            } else {
                differing.add("internal_camera.confidence: $oldConfidence -> $newConfidence")
            }
        }
        
        val oldPassengers = old.passengers
        val newPassengers = new.passengers
        if (oldPassengers != null && newPassengers != null) {
            val passengersResult = comparePassengers(oldPassengers, newPassengers)
            matching.addAll(passengersResult.matching.map { "internal_camera.passengers.$it" })
            differing.addAll(passengersResult.differing.map { "internal_camera.passengers.$it" })
            if (passengersResult.differing.isEmpty()) score += 1.0
        } else if (oldPassengers != newPassengers) {
            differing.add("internal_camera.passengers: 人数变化")
        }
        
        return FieldComparisonResult(matching, differing, score / 3.0)
    }
    
    /**
     * 比较乘客数据 - 关键字段必须完全一致
     */
    private fun comparePassengers(old: Passengers, new: Passengers): FieldComparisonResult {
        val matching = mutableListOf<String>()
        val differing = mutableListOf<String>()
        
        if (old.children == new.children) {
            matching.add("children: ${old.children}")
        } else {
            differing.add("children: ${old.children} -> ${new.children}")
        }
        
        if (old.adults == new.adults) {
            matching.add("adults: ${old.adults}")
        } else {
            differing.add("adults: ${old.adults} -> ${new.adults}")
        }
        
        if (old.seniors == new.seniors) {
            matching.add("seniors: ${old.seniors}")
        } else {
            differing.add("seniors: ${old.seniors} -> ${new.seniors}")
        }
        
        return FieldComparisonResult(matching, differing, if (differing.isEmpty()) 1.0 else 0.0)
    }
    
    /**
     * 比较麦克风数据
     */
    private fun compareInternalMic(old: InternalMic, new: InternalMic): FieldComparisonResult {
        val matching = mutableListOf<String>()
        val differing = mutableListOf<String>()
        var score = 0.0
        var fieldCount = 0
        
        if (old.has_voice == new.has_voice) {
            matching.add("internal_mic.has_voice: ${old.has_voice}")
            score += 1.0
        } else {
            differing.add("internal_mic.has_voice: ${old.has_voice} -> ${new.has_voice}")
        }
        fieldCount++
        
        if (old.voice_count == new.voice_count) {
            matching.add("internal_mic.voice_count: ${old.voice_count}")
            score += 1.0
        } else {
            differing.add("internal_mic.voice_count: ${old.voice_count} -> ${new.voice_count}")
        }
        fieldCount++
        
        val oldVolumeLevel = old.volume_level
        val newVolumeLevel = new.volume_level
        if (oldVolumeLevel != null && newVolumeLevel != null) {
            val diff = abs(oldVolumeLevel - newVolumeLevel)
            if (diff <= 0.1) {
                matching.add("internal_mic.volume_level (误差: ${"%.3f".format(diff)})")
                score += 1.0
            } else {
                differing.add("internal_mic.volume_level: ${"%.3f".format(oldVolumeLevel)} -> ${"%.3f".format(newVolumeLevel)}")
            }
            fieldCount++
        }
        
        val oldNoiseLevel = old.noise_level
        val newNoiseLevel = new.noise_level
        if (oldNoiseLevel != null && newNoiseLevel != null) {
            val diff = abs(oldNoiseLevel - newNoiseLevel)
            if (diff <= 0.1) {
                matching.add("internal_mic.noise_level (误差: ${"%.3f".format(diff)})")
                score += 1.0
            } else {
                differing.add("internal_mic.noise_level: ${"%.3f".format(oldNoiseLevel)} -> ${"%.3f".format(newNoiseLevel)}")
            }
            fieldCount++
        }
        
        return FieldComparisonResult(matching, differing, score / fieldCount)
    }
    
    /**
     * 比较外部摄像头数据
     */
    private fun compareExternalCamera(old: ExternalCamera, new: ExternalCamera): FieldComparisonResult {
        val matching = mutableListOf<String>()
        val differing = mutableListOf<String>()
        var score = 0.0
        var fieldCount = 0
        
        if (old.primary_color == new.primary_color) {
            matching.add("external_camera.primary_color: ${old.primary_color}")
            score += 1.0
        } else {
            differing.add("external_camera.primary_color: ${old.primary_color} -> ${new.primary_color}")
        }
        fieldCount++
        
        if (old.scene_description == new.scene_description) {
            matching.add("external_camera.scene_description: ${old.scene_description}")
            score += 1.0
        } else {
            differing.add("external_camera.scene_description: 变化")
        }
        fieldCount++
        
        return FieldComparisonResult(matching, differing, score / fieldCount)
    }
    
    /**
     * 比较车辆数据
     */
    private fun compareVehicle(old: Vehicle, new: Vehicle): FieldComparisonResult {
        val matching = mutableListOf<String>()
        val differing = mutableListOf<String>()
        var score = 0.0
        var fieldCount = 0
        
        if (old.speed_kmh == new.speed_kmh) {
            matching.add("vehicle.speed_kmh: ${old.speed_kmh}")
            score += 1.0
        } else {
            val diff = abs((old.speed_kmh ?: 0.0) - (new.speed_kmh ?: 0.0))
            if (diff <= 5.0) {
                matching.add("vehicle.speed_kmh (误差: ${"%.1f".format(diff)} km/h)")
                score += 0.8
            } else {
                differing.add("vehicle.speed_kmh: ${old.speed_kmh} -> ${new.speed_kmh}")
            }
        }
        fieldCount++
        
        if (old.gear == new.gear) {
            matching.add("vehicle.gear: ${old.gear}")
            score += 1.0
        } else {
            differing.add("vehicle.gear: ${old.gear} -> ${new.gear}")
        }
        fieldCount++
        
        return FieldComparisonResult(matching, differing, score / fieldCount)
    }
    
    /**
     * 检查是否超时
     */
    private fun shouldTimeout(currentTime: Long): Boolean {
        if (sampleCount >= maxWaitSamples) return true
        if (currentTime - firstSampleTime >= timeoutMs) return true
        return false
    }
    
    /**
     * 计算数据哈希
     */
    private fun computeHash(data: StandardizedSignals): String {
        val sb = StringBuilder()
        data.signals?.internal_camera?.let { cam ->
            sb.append("ic:${cam.mood}:${cam.passengers?.adults}:${cam.passengers?.children}:${cam.passengers?.seniors}")
        }
        data.signals?.internal_mic?.let { mic ->
            sb.append("|im:${mic.has_voice}:${mic.voice_count}")
        }
        data.signals?.external_camera?.let { ext ->
            sb.append("|ec:${ext.primary_color}:${ext.scene_description}")
        }
        return sb.toString().hashCode().toString(16)
    }
    
    /**
     * 重置缓存
     */
    fun reset() {
        lock.withLock {
            cachedSample.set(null)
            sampleCount = 0
            firstSampleTime = 0
        }
    }
    
    /**
     * 获取当前缓存状态
     */
    fun getCacheStatus(): CacheStatus {
        val sample = cachedSample.get()
        return CacheStatus(
            hasCache = sample != null,
            sampleCount = sampleCount,
            firstSampleTime = firstSampleTime,
            currentHash = sample?.hash ?: ""
        )
    }
    
    data class ComparisonResult(
        val isSimilar: Boolean,
        val score: Double,
        val matchingFields: List<String>,
        val differingFields: List<String>
    )
    
    data class FieldComparisonResult(
        val matching: List<String>,
        val differing: List<String>,
        val score: Double
    )
    
    data class CacheStatus(
        val hasCache: Boolean,
        val sampleCount: Int,
        val firstSampleTime: Long,
        val currentHash: String
    )
}
