package com.music.perception.test.mock.generator.impl

import com.music.perception.api.*
import com.music.perception.test.mock.generator.GeneratorConfig
import com.music.perception.test.mock.generator.StandardizedSignalsGenerator
import com.music.perception.test.mock.generator.TestScenario
import kotlin.random.Random

/**
 * StandardizedSignals 数据生成器实现
 * 提供各种场景的数据生成功能
 */
class StandardizedSignalsGeneratorImpl(
    private val config: GeneratorConfig
) : StandardizedSignalsGenerator {
    
    private val random = config.randomSeed?.let { Random(it) } ?: Random.Default
    
    override suspend fun generate(): StandardizedSignals {
        return generateForScenario(config.scenario)
    }
    
    override suspend fun generateForScenario(scenario: TestScenario): StandardizedSignals {
        return when (scenario) {
            TestScenario.NORMAL_DAY -> generateNormalDayData()
            TestScenario.NORMAL_NIGHT -> generateNormalNightData()
            TestScenario.RAINY -> generateRainyData()
            TestScenario.SUNNY -> generateSunnyData()
            TestScenario.BOUNDARY_MAX -> generateBoundaryMaxData()
            TestScenario.BOUNDARY_MIN -> generateBoundaryMinData()
            TestScenario.BOUNDARY_ZERO -> generateBoundaryZeroData()
            TestScenario.EXCEPTION_MISSING_FIELD -> generateMissingFieldData()
            TestScenario.EXCEPTION_NULL_VALUE -> generateNullValueData()
            TestScenario.EXCEPTION_INVALID_FORMAT -> generateInvalidFormatData()
            TestScenario.RANDOM -> generateRandomData()
        }
    }
    
    override fun reset() {
        // 重置生成器状态（如果需要）
    }
    
    override fun getName(): String {
        return "StandardizedSignalsGenerator_${config.scenario.name}"
    }
    
    /**
     * 生成正常白天数据
     */
    private fun generateNormalDayData(): StandardizedSignals {
        return StandardizedSignals(
            timestamp = System.currentTimeMillis(),
            signals = Signals(
                external_camera = ExternalCamera(
                    primary_color = "#87CEEB",
                    secondary_color = "#FFFFFF",
                    brightness = 0.8,
                    scene_description = "Clear day sky with bright sunlight"
                ),
                internal_camera = InternalCamera(
                    mood = "happy",
                    confidence = 0.9,
                    passengers = Passengers(
                        adults = 1,
                        children = 0,
                        seniors = 0
                    )
                ),
                vehicle = Vehicle(
                    speed_kmh = 60.0,
                    fuel_level = 0.75,
                    engine_temperature = 90.0
                ),
                environment = Environment(
                    weather = "sunny",
                    temperature = 25.0,
                    humidity = 0.5
                ),
                mic = MicData(
                    volume = 0.3,
                    hasVoice = false,
                    voiceCount = 0,
                    noiseLevel = 0.2
                )
            ),
            confidence = OverallConfidence(
                overall = 0.85,
                external_camera = 0.9,
                internal_camera = 0.8,
                vehicle = 0.95,
                environment = 0.85,
                mic = 0.75
            )
        )
    }
    
    /**
     * 生成正常夜晚数据
     */
    private fun generateNormalNightData(): StandardizedSignals {
        return StandardizedSignals(
            timestamp = System.currentTimeMillis(),
            signals = Signals(
                external_camera = ExternalCamera(
                    primary_color = "#191970",
                    secondary_color = "#000000",
                    brightness = 0.2,
                    scene_description = "Dark night with street lights"
                ),
                internal_camera = InternalCamera(
                    mood = "tired",
                    confidence = 0.85,
                    passengers = Passengers(
                        adults = 1,
                        children = 0,
                        seniors = 0
                    )
                ),
                vehicle = Vehicle(
                    speed_kmh = 40.0,
                    fuel_level = 0.6,
                    engine_temperature = 85.0
                ),
                environment = Environment(
                    weather = "clear",
                    temperature = 18.0,
                    humidity = 0.6
                ),
                mic = MicData(
                    volume = 0.1,
                    hasVoice = false,
                    voiceCount = 0,
                    noiseLevel = 0.1
                )
            ),
            confidence = OverallConfidence(
                overall = 0.75,
                external_camera = 0.7,
                internal_camera = 0.75,
                vehicle = 0.9,
                environment = 0.8,
                mic = 0.7
            )
        )
    }
    
    /**
     * 生成雨天数据
     */
    private fun generateRainyData(): StandardizedSignals {
        return StandardizedSignals(
            timestamp = System.currentTimeMillis(),
            signals = Signals(
                external_camera = ExternalCamera(
                    primary_color = "#708090",
                    secondary_color = "#2F4F4F",
                    brightness = 0.4,
                    scene_description = "Rainy day with overcast sky"
                ),
                internal_camera = InternalCamera(
                    mood = "neutral",
                    confidence = 0.8,
                    passengers = Passengers(
                        adults = 2,
                        children = 0,
                        seniors = 0
                    )
                ),
                vehicle = Vehicle(
                    speed_kmh = 30.0,
                    fuel_level = 0.5,
                    engine_temperature = 88.0
                ),
                environment = Environment(
                    weather = "rainy",
                    temperature = 15.0,
                    humidity = 0.9
                ),
                mic = MicData(
                    volume = 0.5,
                    hasVoice = true,
                    voiceCount = 2,
                    noiseLevel = 0.6
                )
            ),
            confidence = OverallConfidence(
                overall = 0.7,
                external_camera = 0.65,
                internal_camera = 0.75,
                vehicle = 0.85,
                environment = 0.8,
                mic = 0.6
            )
        )
    }
    
    /**
     * 生成晴天数据
     */
    private fun generateSunnyData(): StandardizedSignals {
        return StandardizedSignals(
            timestamp = System.currentTimeMillis(),
            signals = Signals(
                external_camera = ExternalCamera(
                    primary_color = "#00BFFF",
                    secondary_color = "#FFD700",
                    brightness = 0.95,
                    scene_description = "Bright sunny day with clear blue sky"
                ),
                internal_camera = InternalCamera(
                    mood = "happy",
                    confidence = 0.95,
                    passengers = Passengers(
                        adults = 1,
                        children = 1,
                        seniors = 0
                    )
                ),
                vehicle = Vehicle(
                    speed_kmh = 80.0,
                    fuel_level = 0.9,
                    engine_temperature = 92.0
                ),
                environment = Environment(
                    weather = "sunny",
                    temperature = 32.0,
                    humidity = 0.3
                ),
                mic = MicData(
                    volume = 0.4,
                    hasVoice = true,
                    voiceCount = 3,
                    noiseLevel = 0.3
                )
            ),
            confidence = OverallConfidence(
                overall = 0.9,
                external_camera = 0.95,
                internal_camera = 0.9,
                vehicle = 0.95,
                environment = 0.9,
                mic = 0.8
            )
        )
    }
    
    /**
     * 生成边界最大值数据
     */
    private fun generateBoundaryMaxData(): StandardizedSignals {
        return StandardizedSignals(
            timestamp = Long.MAX_VALUE,
            signals = Signals(
                external_camera = ExternalCamera(
                    primary_color = "#FFFFFF",
                    secondary_color = "#FFFFFF",
                    brightness = 1.0,
                    scene_description = "Maximum brightness scenario"
                ),
                internal_camera = InternalCamera(
                    mood = "excited",
                    confidence = 1.0,
                    passengers = Passengers(
                        adults = Int.MAX_VALUE,
                        children = Int.MAX_VALUE,
                        seniors = Int.MAX_VALUE
                    )
                ),
                vehicle = Vehicle(
                    speed_kmh = Double.MAX_VALUE,
                    fuel_level = 1.0,
                    engine_temperature = Double.MAX_VALUE
                ),
                environment = Environment(
                    weather = "extreme",
                    temperature = Double.MAX_VALUE,
                    humidity = 1.0
                ),
                mic = MicData(
                    volume = 1.0,
                    hasVoice = true,
                    voiceCount = Int.MAX_VALUE,
                    noiseLevel = 1.0
                )
            ),
            confidence = OverallConfidence(
                overall = 1.0,
                external_camera = 1.0,
                internal_camera = 1.0,
                vehicle = 1.0,
                environment = 1.0,
                mic = 1.0
            )
        )
    }
    
    /**
     * 生成边界最小值数据
     */
    private fun generateBoundaryMinData(): StandardizedSignals {
        return StandardizedSignals(
            timestamp = Long.MIN_VALUE,
            signals = Signals(
                external_camera = ExternalCamera(
                    primary_color = "#000000",
                    secondary_color = "#000000",
                    brightness = 0.0,
                    scene_description = "Minimum brightness scenario"
                ),
                internal_camera = InternalCamera(
                    mood = "unknown",
                    confidence = 0.0,
                    passengers = Passengers(
                        adults = 0,
                        children = 0,
                        seniors = 0
                    )
                ),
                vehicle = Vehicle(
                    speed_kmh = 0.0,
                    fuel_level = 0.0,
                    engine_temperature = 0.0
                ),
                environment = Environment(
                    weather = "unknown",
                    temperature = Double.MIN_VALUE,
                    humidity = 0.0
                ),
                mic = MicData(
                    volume = 0.0,
                    hasVoice = false,
                    voiceCount = 0,
                    noiseLevel = 0.0
                )
            ),
            confidence = OverallConfidence(
                overall = 0.0,
                external_camera = 0.0,
                internal_camera = 0.0,
                vehicle = 0.0,
                environment = 0.0,
                mic = 0.0
            )
        )
    }
    
    /**
     * 生成边界零值数据
     */
    private fun generateBoundaryZeroData(): StandardizedSignals {
        return StandardizedSignals(
            timestamp = 0L,
            signals = Signals(
                external_camera = ExternalCamera(
                    primary_color = "#000000",
                    secondary_color = "#000000",
                    brightness = 0.0,
                    scene_description = ""
                ),
                internal_camera = InternalCamera(
                    mood = "",
                    confidence = 0.0,
                    passengers = Passengers(
                        adults = 0,
                        children = 0,
                        seniors = 0
                    )
                ),
                vehicle = Vehicle(
                    speed_kmh = 0.0,
                    fuel_level = 0.0,
                    engine_temperature = 0.0
                ),
                environment = Environment(
                    weather = "",
                    temperature = 0.0,
                    humidity = 0.0
                ),
                mic = MicData(
                    volume = 0.0,
                    hasVoice = false,
                    voiceCount = 0,
                    noiseLevel = 0.0
                )
            ),
            confidence = OverallConfidence(
                overall = 0.0,
                external_camera = 0.0,
                internal_camera = 0.0,
                vehicle = 0.0,
                environment = 0.0,
                mic = 0.0
            )
        )
    }
    
    /**
     * 生成缺失字段数据（模拟异常）
     */
    private fun generateMissingFieldData(): StandardizedSignals {
        // 返回一个部分字段缺失的数据（实际实现中可能需要特殊处理）
        return StandardizedSignals(
            timestamp = System.currentTimeMillis(),
            signals = Signals(
                external_camera = ExternalCamera(
                    primary_color = "",
                    secondary_color = "",
                    brightness = 0.0,
                    scene_description = ""
                ),
                internal_camera = InternalCamera(
                    mood = "",
                    confidence = 0.0,
                    passengers = Passengers(
                        adults = 0,
                        children = 0,
                        seniors = 0
                    )
                ),
                vehicle = Vehicle(
                    speed_kmh = 0.0,
                    fuel_level = 0.0,
                    engine_temperature = 0.0
                ),
                environment = Environment(
                    weather = "",
                    temperature = 0.0,
                    humidity = 0.0
                ),
                mic = MicData(
                    volume = 0.0,
                    hasVoice = false,
                    voiceCount = 0,
                    noiseLevel = 0.0
                )
            ),
            confidence = OverallConfidence(
                overall = 0.0,
                external_camera = 0.0,
                internal_camera = 0.0,
                vehicle = 0.0,
                environment = 0.0,
                mic = 0.0
            )
        )
    }
    
    /**
     * 生成空值数据（模拟异常）
     */
    private fun generateNullValueData(): StandardizedSignals {
        // 返回一个包含空值的数据（实际实现中可能需要特殊处理）
        return generateBoundaryZeroData()
    }
    
    /**
     * 生成格式错误数据（模拟异常）
     */
    private fun generateInvalidFormatData(): StandardizedSignals {
        return StandardizedSignals(
            timestamp = -1L,  // 无效的时间戳
            signals = Signals(
                external_camera = ExternalCamera(
                    primary_color = "INVALID_COLOR",  // 无效的颜色格式
                    secondary_color = "INVALID_COLOR",
                    brightness = 2.0,  // 超出范围的亮度
                    scene_description = "Invalid format data"
                ),
                internal_camera = InternalCamera(
                    mood = "invalid_mood",
                    confidence = 1.5,  // 超出范围的置信度
                    passengers = Passengers(
                        adults = -1,  // 无效的乘客数量
                        children = -1,
                        seniors = -1
                    )
                ),
                vehicle = Vehicle(
                    speed_kmh = -10.0,  // 无效的速度
                    fuel_level = 1.5,  // 超出范围的燃油量
                    engine_temperature = -50.0  // 无效的温度
                ),
                environment = Environment(
                    weather = "invalid_weather",
                    temperature = 1000.0,  // 极端温度
                    humidity = 1.5  // 超出范围的湿度
                ),
                mic = MicData(
                    volume = 2.0,  // 超出范围的音量
                    hasVoice = false,
                    voiceCount = -1,  // 无效的计数
                    noiseLevel = 2.0  // 超出范围的噪音级别
                )
            ),
            confidence = OverallConfidence(
                overall = 1.5,  // 超出范围的置信度
                external_camera = 1.5,
                internal_camera = 1.5,
                vehicle = 1.5,
                environment = 1.5,
                mic = 1.5
            )
        )
    }
    
    /**
     * 生成随机数据
     */
    private fun generateRandomData(): StandardizedSignals {
        val moods = listOf("happy", "sad", "angry", "neutral", "tired", "excited", "stressed")
        val weathers = listOf("sunny", "cloudy", "rainy", "snowy", "foggy", "windy", "clear")
        val scenes = listOf(
            "City street", "Highway", "Rural road", "Parking lot",
            "Residential area", "Commercial district", "Industrial zone"
        )
        
        return StandardizedSignals(
            timestamp = System.currentTimeMillis(),
            signals = Signals(
                external_camera = ExternalCamera(
                    primary_color = generateRandomColor(),
                    secondary_color = generateRandomColor(),
                    brightness = random.nextDouble(0.0, 1.0),
                    scene_description = scenes.random(random)
                ),
                internal_camera = InternalCamera(
                    mood = moods.random(random),
                    confidence = random.nextDouble(0.0, 1.0),
                    passengers = Passengers(
                        adults = random.nextInt(0, 5),
                        children = random.nextInt(0, 3),
                        seniors = random.nextInt(0, 2)
                    )
                ),
                vehicle = Vehicle(
                    speed_kmh = random.nextDouble(0.0, 120.0),
                    fuel_level = random.nextDouble(0.0, 1.0),
                    engine_temperature = random.nextDouble(60.0, 110.0)
                ),
                environment = Environment(
                    weather = weathers.random(random),
                    temperature = random.nextDouble(-10.0, 45.0),
                    humidity = random.nextDouble(0.0, 1.0)
                ),
                mic = MicData(
                    volume = random.nextDouble(0.0, 1.0),
                    hasVoice = random.nextBoolean(),
                    voiceCount = random.nextInt(0, 10),
                    noiseLevel = random.nextDouble(0.0, 1.0)
                )
            ),
            confidence = OverallConfidence(
                overall = random.nextDouble(0.0, 1.0),
                external_camera = random.nextDouble(0.0, 1.0),
                internal_camera = random.nextDouble(0.0, 1.0),
                vehicle = random.nextDouble(0.0, 1.0),
                environment = random.nextDouble(0.0, 1.0),
                mic = random.nextDouble(0.0, 1.0)
            )
        )
    }
    
    /**
     * 生成随机颜色（HEX 格式）
     */
    private fun generateRandomColor(): String {
        val r = random.nextInt(0, 256)
        val g = random.nextInt(0, 256)
        val b = random.nextInt(0, 256)
        return String.format("#%02X%02X%02X", r, g, b)
    }
}
