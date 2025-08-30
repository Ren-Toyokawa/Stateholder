package io.github.rentoyokawa.stateholder.ksp.domain.models

/**
 * StateHolder検証結果を表すデータクラス
 */
data class ValidationResult(
    val isValid: Boolean,
    val errors: List<ValidationError>,
    val warnings: List<String> = emptyList()
)

/**
 * 検証エラーの詳細情報を表すデータクラス
 */
data class ValidationError(
    val type: ErrorType,
    val message: String,
    val field: String? = null
)

/**
 * 検証エラーの種類を表す列挙型
 */
enum class ErrorType {
    MISSING_REQUIRED_FIELD,
    INVALID_TYPE_NAME,
    CIRCULAR_DEPENDENCY,
    INVALID_PACKAGE_NAME,
    INVALID_CLASS_NAME
}