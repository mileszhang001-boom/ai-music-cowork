package com.music.perception.test.mock.validator

/**
 * 数据验证器接口
 * 定义验证数据的统一接口
 */
interface DataValidator<T> {
    
    /**
     * 验证数据
     * @param data 待验证的数据
     * @return 验证结果
     */
    fun validate(data: T): ValidationResult
    
    /**
     * 获取验证器名称
     */
    fun getName(): String
}

/**
 * 验证结果
 */
data class ValidationResult(
    val isValid: Boolean,
    val errors: List<ValidationError> = emptyList(),
    val warnings: List<ValidationWarning> = emptyList()
) {
    /**
     * 合并多个验证结果
     */
    fun combine(other: ValidationResult): ValidationResult {
        return ValidationResult(
            isValid = isValid && other.isValid,
            errors = errors + other.errors,
            warnings = warnings + other.warnings
        )
    }
    
    /**
     * 获取错误消息列表
     */
    fun getErrorMessages(): List<String> {
        return errors.map { it.message }
    }
    
    /**
     * 获取警告消息列表
     */
    fun getWarningMessages(): List<String> {
        return warnings.map { it.message }
    }
}

/**
 * 验证错误
 */
data class ValidationError(
    val field: String,
    val message: String,
    val expectedValue: Any? = null,
    val actualValue: Any? = null,
    val errorType: ValidationErrorType
)

/**
 * 验证警告
 */
data class ValidationWarning(
    val field: String,
    val message: String,
    val warningType: ValidationWarningType
)

/**
 * 验证错误类型
 */
enum class ValidationErrorType {
    MISSING_FIELD,      // 缺失字段
    NULL_VALUE,         // 空值
    INVALID_TYPE,       // 类型错误
    OUT_OF_RANGE,       // 超出范围
    INVALID_FORMAT,     // 格式错误
    CONSTRAINT_VIOLATION // 约束违反
}

/**
 * 验证警告类型
 */
enum class ValidationWarningType {
    UNUSUAL_VALUE,      // 异常值
    DEPRECATED_FIELD,   // 废弃字段
    PERFORMANCE_ISSUE,  // 性能问题
    BEST_PRACTICE       // 最佳实践建议
}

/**
 * 字段完整性验证器
 */
interface FieldCompletenessValidator<T> : DataValidator<T> {
    
    /**
     * 获取必需字段列表
     */
    fun getRequiredFields(): List<String>
    
    /**
     * 检查字段是否存在
     */
    fun checkFieldExists(data: T, field: String): Boolean
}

/**
 * 数据类型验证器
 */
interface DataTypeValidator<T> : DataValidator<T> {
    
    /**
     * 获取字段类型映射
     */
    fun getFieldTypes(): Map<String, Class<*>>
    
    /**
     * 检查字段类型
     */
    fun checkFieldType(data: T, field: String, expectedType: Class<*>): Boolean
}

/**
 * 数据范围验证器
 */
interface DataRangeValidator<T> : DataValidator<T> {
    
    /**
     * 获取字段范围映射
     */
    fun getFieldRanges(): Map<String, ClosedRange<*>>
    
    /**
     * 检查字段值是否在范围内
     */
    fun checkFieldInRange(data: T, field: String, range: ClosedRange<*>): Boolean
}

/**
 * 格式验证器
 */
interface FormatValidator<T> : DataValidator<T> {
    
    /**
     * 获取字段格式映射
     */
    fun getFieldFormats(): Map<String, Regex>
    
    /**
     * 检查字段格式
     */
    fun checkFieldFormat(data: T, field: String, pattern: Regex): Boolean
}
