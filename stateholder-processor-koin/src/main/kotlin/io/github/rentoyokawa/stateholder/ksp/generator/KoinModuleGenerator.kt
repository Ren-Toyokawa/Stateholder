package io.github.rentoyokawa.stateholder.ksp.generator

import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import io.github.rentoyokawa.stateholder.ksp.core.StateHolderAnalysis
import io.github.rentoyokawa.stateholder.ksp.domain.models.ParameterModel

/**
 * KoinモジュールのDSLコードを生成するジェネレータ
 * StateHolderAnalysisのリストから、Koinモジュール定義のコードを文字列として生成する
 * ViewModelインスタンスから動的にSharedStateをマッピングする方式に対応
 */
class KoinModuleGenerator {
    
    // ViewModelとStateHolderのマッピング情報を保持
    data class ViewModelStateHolderMapping(
        val viewModelClass: KSClassDeclaration,
        val stateHolderClass: KSClassDeclaration,
        val sharedStateFields: List<KSPropertyDeclaration>
    )
    
    private val viewModelMappings = mutableListOf<ViewModelStateHolderMapping>()
    
    companion object {
        private val MODULE_CLASS = ClassName("org.koin.core.module", "Module")
        private val MODULE_FUNCTION = MemberName("org.koin.dsl", "module")
        private val FACTORY_FUNCTION = MemberName("org.koin.dsl", "factory")
        private val PARAMETER_SET_CLASS = ClassName("org.koin.core.parameter", "ParametersDefinition")
        private val VIEW_MODEL_CLASS = ClassName("androidx.lifecycle", "ViewModel")
        private val COROUTINE_SCOPE_CLASS = ClassName("kotlinx.coroutines", "CoroutineScope")
    }
    
    /**
     * StateHolderAnalysisのリストからKoinモジュールのコードを生成
     * 
     * @param analyses StateHolder解析結果のリスト
     * @return Koinモジュール定義のコード文字列
     * @throws IllegalArgumentException クラス名、パラメータ名、タイプ名が空の場合
     */
    fun generateModule(analyses: List<StateHolderAnalysis>): String {
        // 包括的な入力検証
        require(analyses.all { it.className.isNotBlank() }) { 
            "className must not be blank" 
        }
        require(analyses.all { analysis -> 
            analysis.factoryParams.all { param ->
                param.name.isNotBlank() && param.typeName.isNotBlank()
            }
        }) { 
            "Parameter names and types must not be blank" 
        }
        
        // shouldGenerateがtrueのものだけをフィルタリング
        val generatableAnalyses = analyses.filter { it.shouldGenerate }
        
        // モジュールプロパティの生成
        val moduleProperty = generateModuleProperty(generatableAnalyses)
        
        // FileSpecを生成して文字列に変換（インポートを適切に設定）
        val fileSpec = FileSpec.builder("", "")
            .addImport("org.koin.dsl", "module")
            .addImport("org.koin.dsl", "factory")
            .addProperty(moduleProperty)
            .build()
        
        // プロパティ定義部分を抽出して返す
        return buildString {
            append("val stateHolderModule = ")
            append(moduleProperty.initializer?.toString() ?: "module {\n}")
        }
    }
    
    /**
     * moduleプロパティを生成
     * 
     * @param analyses フィルタリング済みのStateHolder解析結果
     * @return Koinモジュール定義のPropertySpec
     */
    private fun generateModuleProperty(analyses: List<StateHolderAnalysis>): PropertySpec {
        val moduleBuilder = CodeBlock.builder()
        // MemberNameを使用してmodule関数を参照
        moduleBuilder.add("module {\n")
        moduleBuilder.indent()
        
        analyses.forEach { analysis ->
            val factoryDefinition = generateFactory(analysis)
            moduleBuilder.add("%L\n", factoryDefinition)
        }
        
        moduleBuilder.unindent()
        moduleBuilder.add("}")
        
        return PropertySpec.builder("stateHolderModule", MODULE_CLASS)
            .initializer(moduleBuilder.build())
            .build()
    }
    
    /**
     * 型名を解析してTypeNameを返す
     * 単純な型、ジェネリクス型、関数型などを適切に処理する
     * 
     * @param typeName 解析する型名の文字列
     * @return 解析できた場合はTypeName、できない場合はnull
     */
    private fun parseTypeName(typeName: String): TypeName? {
        return try {
            when {
                // 関数型の処理 (例: "(String) -> Unit")
                typeName.contains("->") -> {
                    parseLambdaType(typeName)
                }
                // ジェネリクス型の処理 (例: "List<Item>")
                typeName.contains("<") && typeName.contains(">") -> {
                    parseParameterizedType(typeName)
                }
                // 配列型の処理 (例: "Array<String>")
                typeName.startsWith("Array<") -> {
                    parseParameterizedType(typeName)
                }
                // 単純な型
                else -> ClassName.bestGuess(typeName)
            }
        } catch (e: Exception) {
            // 解析に失敗した場合は型推論に依存
            null
        }
    }
    
    /**
     * ジェネリクス型を解析してParameterizedTypeNameを返す
     * 例: "List<Item>" -> ParameterizedTypeName
     */
    private fun parseParameterizedType(typeName: String): TypeName {
        val baseTypeEnd = typeName.indexOf('<')
        val baseTypeName = typeName.substring(0, baseTypeEnd).trim()
        val typeArgStart = baseTypeEnd + 1
        val typeArgEnd = typeName.lastIndexOf('>')
        val typeArgName = typeName.substring(typeArgStart, typeArgEnd).trim()
        
        val baseType = ClassName.bestGuess(baseTypeName)
        val typeArg = parseTypeName(typeArgName) ?: STAR
        
        return baseType.parameterizedBy(typeArg)
    }
    
    /**
     * 関数型を解析してLambdaTypeNameを返す
     * 例: "(String) -> Unit" -> LambdaTypeName
     */
    private fun parseLambdaType(typeName: String): TypeName {
        val arrowIndex = typeName.indexOf("->") 
        val paramsString = typeName.substring(1, typeName.lastIndexOf(')'))
        val returnTypeString = typeName.substring(arrowIndex + 2).trim()
        
        // パラメータの解析
        val paramTypes = if (paramsString.isBlank()) {
            emptyList()
        } else {
            paramsString.split(",").map { param ->
                val trimmedParam = param.trim()
                parseSimpleTypeName(trimmedParam) ?: ClassName.bestGuess(trimmedParam)
            }
        }
        
        // 戻り値の型を解析（Unitの場合は正しくUNIT定数を使用）
        val returnType = when (returnTypeString) {
            "Unit", "kotlin.Unit" -> UNIT
            else -> parseSimpleTypeName(returnTypeString) ?: ClassName.bestGuess(returnTypeString)
        }
        
        return LambdaTypeName.get(
            parameters = paramTypes.toTypedArray(),
            returnType = returnType
        )
    }
    
    /**
     * 単純な型名を解析してTypeNameを返す（再帰呼び出しを避けるためのヘルパー）
     */
    private fun parseSimpleTypeName(typeName: String): TypeName? {
        return try {
            when {
                // ジェネリクス型の処理
                typeName.contains("<") && typeName.contains(">") -> {
                    parseParameterizedType(typeName)
                }
                // 配列型の処理
                typeName.startsWith("Array<") -> {
                    parseParameterizedType(typeName)
                }
                // 単純な型
                else -> ClassName.bestGuess(typeName)
            }
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Factory生成ロジック（モジュール内部使用）
     * ViewModelインスタンスを受け取り、動的にSharedStateをマッピングする
     * 
     * @param analysis StateHolder解析結果
     * @return factory定義のコード文字列（インデント付き）
     */
    private fun generateFactory(analysis: StateHolderAnalysis): String {
        val className = analysis.className
        
        // パラメータはViewModelとCoroutineScopeのみを受け取る
        return buildString {
            appendLine("    factory<$className> { params ->")
            appendLine("        val viewModel = params.get<androidx.lifecycle.ViewModel>(0)")
            appendLine("        val scope = params.get<kotlinx.coroutines.CoroutineScope>(1)")
            appendLine("        ")
            
            // SharedStateのマッピングが必要な場合
            val injectedParams = analysis.factoryParams.filter { it.isInjected }
            if (injectedParams.isNotEmpty()) {
                appendLine("        // SharedStateを動的に解決")
                appendLine("        val sharedStates = when (viewModel) {")
                
                // ViewModelマッピングの生成（ここでは仮実装）
                // 実際のマッピング情報はStateHolderProcessorから渡される必要がある
                appendLine("            else -> emptyList<Any>()")
                appendLine("        }")
                appendLine("        ")
            }
            
            // StateHolderのインスタンス生成
            append("        $className(")
            
            val allParams = mutableListOf<String>()
            
            // 注入パラメータの処理
            injectedParams.forEachIndexed { index, param ->
                allParams.add("${param.name} = sharedStates.getOrNull($index) as? ${param.typeName} ?: throw IllegalArgumentException(\"Missing parameter: ${param.name}\")")
            }
            
            // 非注入パラメータ（Koinから取得）
            val nonInjectedParams = analysis.factoryParams.filter { !it.isInjected && it.typeName != "CoroutineScope" && !it.typeName.endsWith(".CoroutineScope") }
            nonInjectedParams.forEach { param ->
                allParams.add("${param.name} = get()")
            }
            
            // scopeパラメータ
            val scopeParams = analysis.factoryParams.filter { 
                it.typeName == "CoroutineScope" || it.typeName.endsWith(".CoroutineScope")
            }
            scopeParams.forEach { param ->
                allParams.add("${param.name} = scope")
            }
            
            if (allParams.isNotEmpty()) {
                appendLine("")
                allParams.forEachIndexed { index, param ->
                    append("            $param")
                    if (index < allParams.size - 1) appendLine(",")
                    else appendLine("")
                }
                append("        )")
            } else {
                append(")")
            }
            
            appendLine("")
            append("    }")
        }
    }
    
    /**
     * Factoryコード生成（公開メソッド、テスト用）
     * インデントなしのfactoryコードを生成
     * 
     * @param analysis StateHolder解析結果
     * @return factory定義のコード文字列
     */
    fun generateFactoryCode(analysis: StateHolderAnalysis): String {
        return buildString {
            val params = generateParams(analysis.factoryParams)
            val args = generateArgs(analysis.factoryParams)
            
            append("factory<${analysis.className}> { ")
            if (params.isNotEmpty()) {
                append("(${params}) ->")
            } else {
                append("->")
            }
            appendLine()
            appendLine("    ${analysis.className}(${args})")
            append("}")
        }
    }
    
    /**
     * パラメータリスト生成（デバッグ・検証用）
     * 
     * @param params パラメータのリスト
     * @return パラメータリスト文字列
     */
    private fun generateParams(params: List<ParameterModel>): String {
        return params.joinToString(", ") { param ->
            "${param.name}: ${param.typeName}"
        }
    }
    
    /**
     * 引数リスト生成（デバッグ・検証用）
     * 
     * @param params パラメータのリスト
     * @return 引数リスト文字列
     */
    private fun generateArgs(params: List<ParameterModel>): String {
        return params.joinToString(", ") { param ->
            param.name
        }
    }
    
    /**
     * 個別のStateHolderに対するfactory定義を生成（旧形式）
     * 
     * @deprecated 新形式のgenerateFactoryを使用してください。このメソッドは次のメジャーバージョンで削除されます。
     * @param analysis StateHolder解析結果
     * @return factory定義のCodeBlock
     */
    @Deprecated(
        message = "新形式のgenerateFactoryを使用してください",
        replaceWith = ReplaceWith("generateFactory(analysis)"),
        level = DeprecationLevel.WARNING
    )
    private fun generateFactoryDefinition(analysis: StateHolderAnalysis): CodeBlock {
        val className = ClassName.bestGuess(analysis.className)
        
        return CodeBlock.builder().apply {
            // factory関数の開始（MemberNameを使用せずに直接文字列として出力）
            add("factory<%T> { params ->\n", className)
            indent()
            
            // StateHolderのインスタンス生成
            add("%T(", className)
            
            if (analysis.factoryParams.isNotEmpty()) {
                add("\n")
                indent()
                
                // ファクトリーパラメータの生成
                analysis.factoryParams.forEachIndexed { index, param ->
                    // 型解析を改善されたメソッドで行う
                    val paramType = parseTypeName(param.typeName)
                    
                    if (paramType != null) {
                        // 型引数を明示
                        add("%N = params.get<%T>()", param.name, paramType)
                    } else {
                        // 型解析が失敗した場合は型推論に依存
                        add("%N = params.get()", param.name)
                    }
                    
                    if (index < analysis.factoryParams.size - 1) {
                        add(",\n")
                    }
                }
                
                add("\n")
                unindent()
            }
            
            add(")\n")
            unindent()
            add("}\n")
        }.build()
    }
}