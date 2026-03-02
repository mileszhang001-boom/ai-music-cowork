package com.music.perception.test.mock.factory

import com.music.perception.test.mock.generator.GeneratorConfig
import com.music.perception.test.mock.generator.MockDataGenerator
import com.music.perception.test.mock.generator.StandardizedSignalsGenerator
import com.music.perception.test.mock.generator.TestScenario

/**
 * Mock 数据工厂类
 * 用于创建不同类型的 Mock 数据生成器
 */
object MockDataFactory {
    
    private val generators = mutableMapOf<String, MockDataGenerator<*>>()
    
    /**
     * 注册生成器
     * @param generator 生成器实例
     */
    fun <T> registerGenerator(generator: MockDataGenerator<T>) {
        generators[generator.getName()] = generator
    }
    
    /**
     * 获取生成器
     * @param name 生成器名称
     * @return 生成器实例
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> getGenerator(name: String): MockDataGenerator<T>? {
        return generators[name] as? MockDataGenerator<T>
    }
    
    /**
     * 获取所有已注册的生成器名称
     * @return 生成器名称列表
     */
    fun getRegisteredGenerators(): List<String> {
        return generators.keys.toList()
    }
    
    /**
     * 注销生成器
     * @param name 生成器名称
     */
    fun unregisterGenerator(name: String) {
        generators.remove(name)
    }
    
    /**
     * 清空所有生成器
     */
    fun clearAll() {
        generators.clear()
    }
    
    /**
     * 创建 StandardizedSignals 生成器
     * @param config 生成器配置
     * @return 生成器实例
     */
    fun createStandardizedSignalsGenerator(config: GeneratorConfig = GeneratorConfig()): StandardizedSignalsGenerator {
        return StandardizedSignalsGeneratorImpl(config)
    }
    
    /**
     * 创建指定场景的生成器
     * @param scenario 测试场景
     * @return 生成器实例
     */
    fun createScenarioGenerator(scenario: TestScenario): StandardizedSignalsGenerator {
        val config = GeneratorConfig(scenario = scenario)
        return createStandardizedSignalsGenerator(config)
    }
    
    /**
     * 创建随机数据生成器
     * @param seed 随机种子
     * @return 生成器实例
     */
    fun createRandomGenerator(seed: Long? = null): StandardizedSignalsGenerator {
        val config = GeneratorConfig(
            scenario = TestScenario.RANDOM,
            randomSeed = seed
        )
        return createStandardizedSignalsGenerator(config)
    }
    
    /**
     * 创建边界值生成器
     * @param boundaryType 边界类型（"max", "min", "zero"）
     * @return 生成器实例
     */
    fun createBoundaryGenerator(boundaryType: String): StandardizedSignalsGenerator {
        val scenario = when (boundaryType.lowercase()) {
            "max" -> TestScenario.BOUNDARY_MAX
            "min" -> TestScenario.BOUNDARY_MIN
            "zero" -> TestScenario.BOUNDARY_ZERO
            else -> TestScenario.NORMAL_DAY
        }
        return createScenarioGenerator(scenario)
    }
    
    /**
     * 创建异常数据生成器
     * @param exceptionType 异常类型（"missing", "null", "invalid"）
     * @return 生成器实例
     */
    fun createExceptionGenerator(exceptionType: String): StandardizedSignalsGenerator {
        val scenario = when (exceptionType.lowercase()) {
            "missing" -> TestScenario.EXCEPTION_MISSING_FIELD
            "null" -> TestScenario.EXCEPTION_NULL_VALUE
            "invalid" -> TestScenario.EXCEPTION_INVALID_FORMAT
            else -> TestScenario.NORMAL_DAY
        }
        return createScenarioGenerator(scenario)
    }
}

/**
 * StandardizedSignals 生成器实现
 */
private class StandardizedSignalsGeneratorImpl(
    private val config: GeneratorConfig
) : StandardizedSignalsGenerator {
    
    override suspend fun generate(): com.music.perception.api.StandardizedSignals {
        return generateForScenario(config.scenario)
    }
    
    override suspend fun generateForScenario(scenario: TestScenario): com.music.perception.api.StandardizedSignals {
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
        // 重置生成器状态
    }
    
    override fun getName(): String {
        return "StandardizedSignalsGenerator_${config.scenario.name}"
    }
    
    private fun generateNormalDayData(): com.music.perception.api.StandardizedSignals {
        // TODO: 实现正常白天数据生成
        return createMockStandardizedSignals()
    }
    
    private fun generateNormalNightData(): com.music.perception.api.StandardizedSignals {
        // TODO: 实现正常夜晚数据生成
        return createMockStandardizedSignals()
    }
    
    private fun generateRainyData(): com.music.perception.api.StandardizedSignals {
        // TODO: 实现雨天数据生成
        return createMockStandardizedSignals()
    }
    
    private fun generateSunnyData(): com.music.perception.api.StandardizedSignals {
        // TODO: 实现晴天数据生成
        return createMockStandardizedSignals()
    }
    
    private fun generateBoundaryMaxData(): com.music.perception.api.StandardizedSignals {
        // TODO: 实现边界最大值数据生成
        return createMockStandardizedSignals()
    }
    
    private fun generateBoundaryMinData(): com.music.perception.api.StandardizedSignals {
        // TODO: 实现边界最小值数据生成
        return createMockStandardizedSignals()
    }
    
    private fun generateBoundaryZeroData(): com.music.perception.api.StandardizedSignals {
        // TODO: 实现边界零值数据生成
        return createMockStandardizedSignals()
    }
    
    private fun generateMissingFieldData(): com.music.perception.api.StandardizedSignals {
        // TODO: 实现缺失字段数据生成
        return createMockStandardizedSignals()
    }
    
    private fun generateNullValueData(): com.music.perception.api.StandardizedSignals {
        // TODO: 实现空值数据生成
        return createMockStandardizedSignals()
    }
    
    private fun generateInvalidFormatData(): com.music.perception.api.StandardizedSignals {
        // TODO: 实现格式错误数据生成
        return createMockStandardizedSignals()
    }
    
    private fun generateRandomData(): com.music.perception.api.StandardizedSignals {
        // TODO: 实现随机数据生成
        return createMockStandardizedSignals()
    }
    
    private fun createMockStandardizedSignals(): com.music.perception.api.StandardizedSignals {
        // 创建一个基本的 Mock StandardizedSignals 对象
        // 这里需要根据实际的 StandardizedSignals 数据结构来实现
        return com.music.perception.api.StandardizedSignals(
            timestamp = System.currentTimeMillis(),
            signals = com.music.perception.api.Signals(
                external_camera = com.music.perception.api.ExternalCamera(
                    primary_color = "#87CEEB",
                    secondary_color = "#FFFFFF",
                    brightness = 0.8,
                    scene_description = "Clear day sky"
                ),
                internal_camera = com.music.perception.api.InternalCamera(
                    mood = "neutral",
                    confidence = 0.9,
                    passengers = com.music.perception.api.Passengers(
                        adults = 1,
                        children = 0,
                        seniors = 0
                    )
                ),
                vehicle = com.music.perception.api.Vehicle(
                    speed_kmh = 60.0,
                    fuel_level = 0.75,
                    engine_temperature = 90.0
                ),
                environment = com.music.perception.api.Environment(
                    weather = "sunny",
                    temperature = 25.0,
                    humidity = 0.5
                ),
                mic = com.music.perception.api.MicData(
                    volume = 0.3,
                    hasVoice = false,
                    voiceCount = 0,
                    noiseLevel = 0.2
                )
            ),
            confidence = com.music.perception.api.OverallConfidence(
                overall = 0.85,
                external_camera = 0.9,
                internal_camera = 0.8,
                vehicle = 0.95,
                environment = 0.85,
                mic = 0.75
            )
        )
    }
}
