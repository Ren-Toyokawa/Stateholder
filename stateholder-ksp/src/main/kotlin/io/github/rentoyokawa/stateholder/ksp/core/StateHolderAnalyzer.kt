package io.github.rentoyokawa.stateholder.ksp.core

import io.github.rentoyokawa.stateholder.ksp.domain.models.ErrorType
import io.github.rentoyokawa.stateholder.ksp.domain.models.ParameterModel
import io.github.rentoyokawa.stateholder.ksp.domain.models.StateHolderModel
import io.github.rentoyokawa.stateholder.ksp.domain.models.ValidationError
import io.github.rentoyokawa.stateholder.ksp.domain.models.ValidationResult

/**
 * StateHolderクラスの解析を行うアナライザー
 */
class StateHolderAnalyzer {
    /**
     * StateHolderModelを解析してStateHolderAnalysisを生成
     */
    fun analyzeStateHolder(model: StateHolderModel): StateHolderAnalysis {
        val factoryParams = extractFactoryParameters(model)
        val shouldGenerate = determineShouldGenerate(model)
        
        return StateHolderAnalysis(
            moduleName = generateModuleName(model),
            className = model.className,
            factoryParams = factoryParams,
            shouldGenerate = shouldGenerate
        )
    }
    
    /**
     * ファクトリー関数に必要なパラメータを抽出
     * injectableでないパラメータのみを抽出
     */
    private fun extractFactoryParameters(model: StateHolderModel): List<ParameterModel> {
        return model.constructorParams.filterNot { it.isInjected }
    }
    
    /**
     * コード生成を行うべきかを判定
     * injectableがtrueの場合のみ生成対象とする
     */
    private fun determineShouldGenerate(model: StateHolderModel): Boolean {
        return model.isInjectable
    }
    
    /**
     * モジュール名を生成
     * クラス名から"StateHolder"サフィックスを除去してモジュール名とする
     */
    private fun generateModuleName(model: StateHolderModel): String {
        val baseName = model.className.removeSuffix("StateHolder")
        return "${baseName}Module"
    }
    
    /**
     * 注入可能なパラメータをフィルタリングして返す
     * isInjectedがtrueのパラメータのみを抽出
     */
    fun findInjectableParameters(params: List<ParameterModel>): List<ParameterModel> {
        return params.filter { it.isInjected }
    }
    
    /**
     * StateHolderModelの検証を実行
     * 必須パラメータ、型の整合性、循環依存などをチェック
     */
    fun validateStateHolder(model: StateHolderModel): ValidationResult {
        val errors = mutableListOf<ValidationError>()
        val warnings = mutableListOf<String>()
        
        // 必須パラメータチェック
        if (model.className.isBlank()) {
            errors.add(
                ValidationError(
                    type = ErrorType.MISSING_REQUIRED_FIELD,
                    message = "className is required and cannot be empty",
                    field = "className"
                )
            )
        }
        
        if (model.packageName.isBlank()) {
            errors.add(
                ValidationError(
                    type = ErrorType.MISSING_REQUIRED_FIELD,
                    message = "packageName is required and cannot be empty",
                    field = "packageName"
                )
            )
        }
        
        // クラス名の妥当性チェック
        if (model.className.isNotBlank() && !isValidClassName(model.className)) {
            errors.add(
                ValidationError(
                    type = ErrorType.INVALID_CLASS_NAME,
                    message = "Invalid class name: ${model.className}",
                    field = "className"
                )
            )
        }
        
        // パッケージ名の妥当性チェック
        if (model.packageName.isNotBlank() && !isValidPackageName(model.packageName)) {
            errors.add(
                ValidationError(
                    type = ErrorType.INVALID_PACKAGE_NAME,
                    message = "Invalid package name: ${model.packageName}",
                    field = "packageName"
                )
            )
        }
        
        // コンストラクタパラメータの型名チェック
        model.constructorParams.forEach { param ->
            if (!isValidTypeName(param.typeName)) {
                errors.add(
                    ValidationError(
                        type = ErrorType.INVALID_TYPE_NAME,
                        message = "Invalid type name '${param.typeName}' for parameter '${param.name}'",
                        field = param.name
                    )
                )
            }
            
            // 循環依存チェック（直接的な自己参照）
            if (param.isInjected && isCircularDependency(model.className, param.typeName)) {
                errors.add(
                    ValidationError(
                        type = ErrorType.CIRCULAR_DEPENDENCY,
                        message = "Circular dependency detected: ${model.className} depends on ${param.typeName}",
                        field = param.name
                    )
                )
            }
        }
        
        // SharedStateの型名チェック
        model.sharedStates.forEach { state ->
            if (!isValidTypeName(state.typeName)) {
                errors.add(
                    ValidationError(
                        type = ErrorType.INVALID_TYPE_NAME,
                        message = "Invalid type name '${state.typeName}' for shared state '${state.propertyName}'",
                        field = state.propertyName
                    )
                )
            }
        }
        
        // 警告の生成
        if (model.className.isNotBlank() && !model.className.endsWith("StateHolder")) {
            warnings.add("Class name '${model.className}' does not follow the convention of ending with 'StateHolder'")
        }
        
        model.constructorParams.forEach { param ->
            if (param.name.length > 40) {
                warnings.add("Parameter name '${param.name}' is longer than recommended (40 characters)")
            }
        }
        
        return ValidationResult(
            isValid = errors.isEmpty(),
            errors = errors,
            warnings = warnings
        )
    }
    
    /**
     * 有効なKotlinクラス名かチェック
     */
    private fun isValidClassName(name: String): Boolean {
        if (name.isEmpty()) return false
        if (!name.first().isLetter() && name.first() != '_') return false
        return name.all { it.isLetterOrDigit() || it == '_' || it == '$' }
    }
    
    /**
     * 有効なパッケージ名かチェック
     */
    private fun isValidPackageName(name: String): Boolean {
        if (name.isEmpty()) return false
        val parts = name.split('.')
        return parts.all { part ->
            part.isNotEmpty() &&
            part.first().isLetter() &&
            part.all { it.isLetterOrDigit() || it == '_' }
        }
    }
    
    /**
     * 有効な型名かチェック
     * ジェネリクス、ラムダ式、nullable型などを考慮
     */
    private fun isValidTypeName(typeName: String): Boolean {
        if (typeName.isEmpty()) return false
        
        // 特殊な型パターンの処理
        when {
            // ラムダ式パターン: (Type) -> ReturnType または suspend (Type) -> ReturnType
            typeName.contains("->") -> {
                return isValidLambdaType(typeName)
            }
            // ジェネリクス型: Type<GenericType>
            typeName.contains('<') && typeName.contains('>') -> {
                return isValidGenericType(typeName)
            }
            // Nullable型: Type?
            typeName.endsWith('?') -> {
                return isValidTypeName(typeName.dropLast(1))
            }
            // 通常の型名
            else -> {
                // 型名は文字またはアンダースコアで始まる必要がある
                if (!typeName.first().isLetter() && typeName.first() != '_') return false
                
                // ドット区切りの完全修飾名をサポート
                val parts = typeName.split('.')
                return parts.all { part ->
                    part.isNotEmpty() &&
                    (part.first().isLetter() || part.first() == '_') &&
                    part.all { it.isLetterOrDigit() || it == '_' || it == '$' }
                }
            }
        }
    }
    
    /**
     * ラムダ型の妥当性チェック
     */
    private fun isValidLambdaType(typeName: String): Boolean {
        // suspend修飾子を除去
        val typeWithoutSuspend = typeName.removePrefix("suspend ").trim()
        
        // "->" で分割
        val parts = typeWithoutSuspend.split("->")
        if (parts.size != 2) return false
        
        val paramsPart = parts[0].trim()
        val returnPart = parts[1].trim()
        
        // パラメータ部分の検証（括弧で囲まれている必要がある）
        if (!paramsPart.startsWith('(') || !paramsPart.endsWith(')')) return false
        
        val paramsContent = paramsPart.drop(1).dropLast(1).trim()
        if (paramsContent.isNotEmpty()) {
            // パラメータがある場合、各パラメータ型を検証
            val params = paramsContent.split(',').map { it.trim() }
            if (!params.all { isValidTypeName(it) }) return false
        }
        
        // 戻り値型の検証
        return isValidTypeName(returnPart)
    }
    
    /**
     * ジェネリック型の妥当性チェック
     */
    private fun isValidGenericType(typeName: String): Boolean {
        val openIndex = typeName.indexOf('<')
        val closeIndex = typeName.lastIndexOf('>')
        
        if (openIndex == -1 || closeIndex == -1 || openIndex >= closeIndex) return false
        
        val baseType = typeName.substring(0, openIndex)
        val genericPart = typeName.substring(openIndex + 1, closeIndex)
        
        // ベース型の検証
        if (!isValidSimpleTypeName(baseType)) return false
        
        // ジェネリックパラメータの検証
        val genericParams = splitGenericParameters(genericPart)
        return genericParams.all { param ->
            val trimmed = param.trim()
            trimmed == "*" || isValidTypeName(trimmed)
        }
    }
    
    /**
     * シンプルな型名の妥当性チェック（ジェネリクスや特殊記号を含まない）
     */
    private fun isValidSimpleTypeName(typeName: String): Boolean {
        if (typeName.isEmpty()) return false
        val parts = typeName.split('.')
        return parts.all { part ->
            part.isNotEmpty() &&
            (part.first().isLetter() || part.first() == '_') &&
            part.all { it.isLetterOrDigit() || it == '_' || it == '$' }
        }
    }
    
    /**
     * ジェネリックパラメータを正しく分割
     * ネストされたジェネリクスを考慮
     */
    private fun splitGenericParameters(genericPart: String): List<String> {
        val result = mutableListOf<String>()
        var current = StringBuilder()
        var depth = 0
        
        for (char in genericPart) {
            when (char) {
                '<' -> {
                    depth++
                    current.append(char)
                }
                '>' -> {
                    depth--
                    current.append(char)
                }
                ',' -> {
                    if (depth == 0) {
                        result.add(current.toString())
                        current = StringBuilder()
                    } else {
                        current.append(char)
                    }
                }
                else -> current.append(char)
            }
        }
        
        if (current.isNotEmpty()) {
            result.add(current.toString())
        }
        
        return result
    }
    
    /**
     * 循環依存のチェック
     * 簡易版: 直接的な自己参照のみチェック
     */
    private fun isCircularDependency(currentClass: String, dependencyType: String): Boolean {
        // 型名からジェネリクスやnullable記号を除去して比較
        val cleanDependencyType = dependencyType
            .substringBefore('<')
            .removeSuffix("?")
            .substringAfterLast('.')
        
        val cleanCurrentClass = currentClass.substringAfterLast('.')
        
        return cleanDependencyType == cleanCurrentClass
    }
}

/**
 * StateHolder解析結果を表すデータクラス
 */
data class StateHolderAnalysis(
    val moduleName: String,
    val className: String,
    val factoryParams: List<ParameterModel>,
    val shouldGenerate: Boolean
)