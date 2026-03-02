package com.music.perception.test.mock.generator

import com.music.perception.api.StandardizedSignals

/**
 * Mock 数据生成器接口
 * 定义生成测试数据的统一接口
 */
interface MockDataGenerator<T> {
    
    /**
     * 生成单个测试数据
     * @return 生成的测试数据
     */
    suspend fun generate(): T
    
    /**
     * 批量生成测试数据
     * @param count 生成数量
     * @return 生成的测试数据列表
     */
    suspend fun generateBatch(count: Int): List<T> {
        return List(count) { generate() }
    }
    
    /**
     * 重置生成器状态
     */
    fun reset()
    
    /**
     * 获取生成器名称
     */
    fun getName(): String
}

/**
 * StandardizedSignals 数据生成器接口
 */
interface StandardizedSignalsGenerator : MockDataGenerator<StandardizedSignals> {
    
    /**
     * 生成指定场景的数据
     * @param scenario 场景类型
     * @return 生成的测试数据
     */
    suspend fun generateForScenario(scenario: TestScenario): StandardizedSignals
}

/**
 * 测试场景枚举
 */
enum class TestScenario {
    NORMAL_DAY,         // 正常白天场景
    NORMAL_NIGHT,       // 正常夜晚场景
    RAINY,              // 雨天场景
    SUNNY,              // 晴天场景
    BOUNDARY_MAX,       // 边界值：最大值
    BOUNDARY_MIN,       // 边界值：最小值
    BOUNDARY_ZERO,      // 边界值：零值
    EXCEPTION_MISSING_FIELD,    // 异常：缺失字段
    EXCEPTION_NULL_VALUE,       // 异常：空值
    EXCEPTION_INVALID_FORMAT,   // 异常：格式错误
    RANDOM              // 随机场景
}

/**
 * 数据生成配置
 */
data class GeneratorConfig(
    val scenario: TestScenario = TestScenario.NORMAL_DAY,
    val randomSeed: Long? = null,
    val customValues: Map<String, Any> = emptyMap()
)
