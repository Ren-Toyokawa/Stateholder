package io.github.rentoyokawa.stateholder.ksp

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSValueParameter
import com.squareup.kotlinpoet.*
import java.io.OutputStream

/**
 * StateHolderのKoinモジュールを自動生成するジェネレータ
 */
class StateHolderModuleGenerator(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger
) {
    
    companion object {
        private val MODULE_CLASS = ClassName("org.koin.core.module", "Module")
        private val MODULE_FUNCTION = MemberName("org.koin.dsl", "module")
        private val COROUTINE_SCOPE_CLASS = ClassName("kotlinx.coroutines", "CoroutineScope")
        private val VIEW_MODEL_CLASS = ClassName("androidx.lifecycle", "ViewModel")
        private val ANY_TYPE = ClassName("kotlin", "Any")
    }
    
    /**
     * StateHolderクラスリストからKoinモジュールを生成
     */
    fun generateStateHolderModule(
        stateHolderClasses: List<KSClassDeclaration>,
        packageName: String = "io.github.rentoyokawa.stateholder.koin.generated"
    ) {
        if (stateHolderClasses.isEmpty()) {
            logger.info("StateHolderクラスが見つからないためKoinモジュール生成をスキップします")
            return
        }
        
        logger.info("[ksp] Koinモジュール生成開始")
        
        val fileName = "StateHolderModule_Generated"
        
        val moduleProperty = generateModuleProperty(stateHolderClasses)
        
        val fileSpec = FileSpec.builder(packageName, fileName)
            .addFileComment("@file:Suppress(\"UNUSED\")")
            .addImport("androidx.lifecycle", "ViewModel")
            .addImport("kotlinx.coroutines", "CoroutineScope")
            .addImport("org.koin.core.module", "Module")
            .addImport("org.koin.dsl", "module")
            .addProperty(moduleProperty)
            .build()
        
        val file = codeGenerator.createNewFile(
            dependencies = Dependencies(false, *stateHolderClasses.mapNotNull { it.containingFile }.toTypedArray()),
            packageName = packageName,
            fileName = fileName
        )
        
        file.use { outputStream ->
            outputStream.write(fileSpec.toString().toByteArray())
        }
        
        logger.info("[ksp] Koinモジュール生成完了: StateHolderModule_Generated")
    }
    
    /**
     * moduleプロパティを生成
     */
    private fun generateModuleProperty(stateHolderClasses: List<KSClassDeclaration>): PropertySpec {
        val moduleBuilder = CodeBlock.builder()
        moduleBuilder.add("%M {\n", MODULE_FUNCTION)
        moduleBuilder.indent()
        
        stateHolderClasses.forEach { stateHolderClass ->
            val factoryDefinition = generateFactoryDefinition(stateHolderClass)
            moduleBuilder.add("%L", factoryDefinition)
        }
        
        moduleBuilder.unindent()
        moduleBuilder.add("}")
        
        return PropertySpec.builder("stateHolderModule", MODULE_CLASS)
            .initializer(moduleBuilder.build())
            .build()
    }
    
    /**
     * ViewModelマッピングを含むKoinモジュールを生成
     */
    fun generateStateHolderModuleWithMappings(
        stateHolderClasses: List<KSClassDeclaration>,
        viewModelMappings: Map<String, List<Pair<KSClassDeclaration, List<KSPropertyDeclaration>>>>,
        packageName: String = "io.github.rentoyokawa.stateholder.koin.generated",
        moduleName: String = "generatedStateHolderModule"
    ) {
        if (stateHolderClasses.isEmpty()) {
            logger.info("StateHolderクラスが見つからないためKoinモジュール生成をスキップします")
            return
        }
        
        logger.info("[ksp] Koinモジュール生成開始（マッピング付き）")
        
        val fileName = "StateHolderModule_Generated"
        
        val moduleProperty = generateModulePropertyWithMappings(stateHolderClasses, viewModelMappings, moduleName)
        
        val fileSpec = FileSpec.builder(packageName, fileName)
            .addFileComment("@file:Suppress(\"UNUSED\")")
            .addImport("androidx.lifecycle", "ViewModel")
            .addImport("kotlinx.coroutines", "CoroutineScope")
            .addImport("org.koin.core.module", "Module")
            .addImport("org.koin.dsl", "module")
            .addProperty(moduleProperty)
            .build()
        
        val file = codeGenerator.createNewFile(
            dependencies = Dependencies(false, *stateHolderClasses.mapNotNull { it.containingFile }.toTypedArray()),
            packageName = packageName,
            fileName = fileName
        )
        
        file.use { outputStream ->
            outputStream.write(fileSpec.toString().toByteArray())
        }
        
        logger.info("[ksp] Koinモジュール生成完了: StateHolderModule_Generated")
    }
    
    /**
     * ViewModelマッピングを含むmoduleプロパティを生成
     */
    private fun generateModulePropertyWithMappings(
        stateHolderClasses: List<KSClassDeclaration>,
        viewModelMappings: Map<String, List<Pair<KSClassDeclaration, List<KSPropertyDeclaration>>>>,
        moduleName: String = "generatedStateHolderModule"
    ): PropertySpec {
        val moduleBuilder = CodeBlock.builder()
        moduleBuilder.add("%M {\n", MODULE_FUNCTION)
        moduleBuilder.indent()
        
        stateHolderClasses.forEach { stateHolderClass ->
            val stateHolderName = stateHolderClass.qualifiedName?.asString()
            val mappings = viewModelMappings[stateHolderName] ?: emptyList()
            val factoryDefinition = generateFactoryDefinitionWithMapping(stateHolderClass, mappings)
            moduleBuilder.add("%L", factoryDefinition)
        }
        
        moduleBuilder.unindent()
        moduleBuilder.add("}")
        
        return PropertySpec.builder(moduleName, MODULE_CLASS)
            .initializer(moduleBuilder.build())
            .build()
    }
    
    /**
     * ViewModelマッピングを含む個別のfactory定義を生成
     */
    private fun generateFactoryDefinitionWithMapping(
        stateHolderClass: KSClassDeclaration,
        viewModelMappings: List<Pair<KSClassDeclaration, List<KSPropertyDeclaration>>>
    ): CodeBlock {
        val className = ClassName(
            stateHolderClass.packageName.asString(),
            stateHolderClass.simpleName.asString()
        )
        
        val injectedParams = getInjectedParameters(stateHolderClass)
        logger.info("[ksp]   factory生成: ${stateHolderClass.simpleName.asString()}, パラメータ数: ${injectedParams.size}")
        
        val factoryBuilder = CodeBlock.builder()
        factoryBuilder.add("  factory<%T> { params ->\n", className)
        factoryBuilder.indent()
        factoryBuilder.indent()
        
        // ViewModelとCoroutineScopeを取得
        factoryBuilder.add("val viewModel = params.get<%T>(0)\n", VIEW_MODEL_CLASS)
        factoryBuilder.add("val scope = params.get<%T>(1)\n", COROUTINE_SCOPE_CLASS)
        
        // SharedStateの動的マッピング
        if (injectedParams.isNotEmpty() && viewModelMappings.isNotEmpty()) {
            factoryBuilder.add("\n")
            factoryBuilder.add("when (viewModel) {\n")
            factoryBuilder.indent()
            
            // 各ViewModelに対するマッピングを生成
            viewModelMappings.forEach { (viewModelClass, sharedStateFields) ->
                val viewModelClassName = ClassName(
                    viewModelClass.packageName.asString(),
                    viewModelClass.simpleName.asString()
                )
                
                factoryBuilder.add("is %T -> %T(\n", viewModelClassName, className)
                factoryBuilder.indent()
                
                // コンストラクタパラメータの構築
                val primaryConstructor = stateHolderClass.primaryConstructor
                if (primaryConstructor != null) {
                    val params = mutableListOf<String>()
                    primaryConstructor.parameters.forEach { param ->
                        when {
                            // @InjectedParamアノテーションが付いているパラメータ
                            param.annotations.any { it.shortName.asString() == "InjectedParam" } -> {
                                val paramType = param.type.resolve().declaration.qualifiedName?.asString()
                                val matchingState = sharedStateFields.find { state ->
                                    state.type.resolve().declaration.qualifiedName?.asString() == paramType
                                }
                                if (matchingState != null) {
                                    params.add("${param.name?.asString()} = viewModel.${matchingState.simpleName.asString()}")
                                }
                            }
                            // scopeパラメータ
                            param.type.resolve().declaration.qualifiedName?.asString() == "kotlinx.coroutines.CoroutineScope" -> {
                                params.add("${param.name?.asString()} = scope")
                            }
                            // その他のパラメータ（Koinから自動解決）
                            else -> {
                                params.add("${param.name?.asString()} = get()")
                            }
                        }
                    }
                    params.forEachIndexed { index, param ->
                        factoryBuilder.add(param)
                        if (index < params.size - 1) {
                            factoryBuilder.add(",\n")
                        }
                    }
                }
                
                factoryBuilder.add("\n")
                factoryBuilder.unindent()
                factoryBuilder.add(")\n")
            }
            
            factoryBuilder.add("else -> throw IllegalArgumentException(\n")
            factoryBuilder.indent()
            factoryBuilder.add("%S\n", "No mapping for ${stateHolderClass.simpleName.asString()} in \${viewModel::class.simpleName}")
            factoryBuilder.unindent()
            factoryBuilder.add(")\n")
            factoryBuilder.unindent()
            factoryBuilder.add("}\n")
        } else {
            // パラメータがない場合のシンプルな生成
            factoryBuilder.add("\n")
            factoryBuilder.add("%T(\n", className)
            factoryBuilder.indent()
            
            val primaryConstructor = stateHolderClass.primaryConstructor
            if (primaryConstructor != null) {
                val params = mutableListOf<String>()
                primaryConstructor.parameters.forEach { param ->
                    when {
                        // scopeパラメータ
                        param.type.resolve().declaration.qualifiedName?.asString() == "kotlinx.coroutines.CoroutineScope" -> {
                            params.add("scope = scope")
                        }
                        // その他のパラメータ（Koinから自動解決）
                        else -> {
                            params.add("${param.name?.asString()} = get()")
                        }
                    }
                }
                params.forEachIndexed { index, param ->
                    factoryBuilder.add(param)
                    if (index < params.size - 1) {
                        factoryBuilder.add(",\n")
                    }
                }
            }
            
            factoryBuilder.add("\n")
            factoryBuilder.unindent()
            factoryBuilder.add(")\n")
        }
        
        factoryBuilder.unindent()
        factoryBuilder.unindent()
        factoryBuilder.add("  }\n\n")
        
        return factoryBuilder.build()
    }
    
    /**
     * 個別のfactory定義を生成（旧バージョン互換）
     */
    private fun generateFactoryDefinition(stateHolderClass: KSClassDeclaration): CodeBlock {
        val className = ClassName(
            stateHolderClass.packageName.asString(),
            stateHolderClass.simpleName.asString()
        )
        
        val injectedParams = getInjectedParameters(stateHolderClass)
        logger.info("[ksp]   factory生成: ${stateHolderClass.simpleName.asString()}, パラメータ数: ${injectedParams.size}")
        
        val factoryBuilder = CodeBlock.builder()
        factoryBuilder.add("factory<%T> { (scope: %T", className, COROUTINE_SCOPE_CLASS)
        
        // @InjectedParamパラメータを追加
        injectedParams.forEach { param ->
            val paramTypeName = getParameterTypeName(param)
            val paramName = getInjectedParamKey(param)
            factoryBuilder.add(", $paramName: %T", paramTypeName)
        }
        
        factoryBuilder.add(") ->\n")
        factoryBuilder.indent()
        factoryBuilder.add("%T(", className)
        
        // コンストラクタの実際の順序でパラメータを構築
        val constructorParams = mutableListOf<String>()
        val primaryConstructor = stateHolderClass.primaryConstructor
        
        if (primaryConstructor != null) {
            primaryConstructor.parameters.forEach { param ->
                when {
                    // @InjectedParamアノテーションが付いているパラメータ
                    param.annotations.any { it.shortName.asString() == "InjectedParam" } -> {
                        val paramName = getInjectedParamKey(param)
                        constructorParams.add(paramName)
                    }
                    // scopeパラメータ（CoroutineScope型）
                    param.type.resolve().declaration.qualifiedName?.asString() == "kotlinx.coroutines.CoroutineScope" -> {
                        constructorParams.add("scope")
                    }
                    // その他のパラメータ（通常はKoinから自動解決される）
                    else -> {
                        // Koinの自動解決に依存（get()を使用）
                        constructorParams.add("get()")
                    }
                }
            }
        }
        
        factoryBuilder.add(constructorParams.joinToString(", "))
        factoryBuilder.add(")\n")
        factoryBuilder.unindent()
        factoryBuilder.add("}\n")
        
        return factoryBuilder.build()
    }
    
    /**
     * StateHolderクラスの@InjectedParamパラメータを取得
     */
    private fun getInjectedParameters(stateHolderClass: KSClassDeclaration): List<KSValueParameter> {
        return stateHolderClass.primaryConstructor?.parameters?.filter { param ->
            param.annotations.any { annotation ->
                annotation.shortName.asString() == "InjectedParam"
            }
        } ?: emptyList()
    }
    
    /**
     * パラメータの型名を取得
     */
    private fun getParameterTypeName(parameter: KSValueParameter): TypeName {
        val resolvedType = parameter.type.resolve()
        return resolvedType.declaration.qualifiedName?.asString()?.let { qualifiedName ->
            ClassName.bestGuess(qualifiedName)
        } ?: ANY_TYPE
    }
    
    /**
     * @InjectedParamアノテーションからキー値を取得
     */
    private fun getInjectedParamKey(parameter: KSValueParameter): String {
        val annotation = parameter.annotations.find { 
            it.shortName.asString() == "InjectedParam" 
        }
        
        // @InjectedParamアノテーションの値を取得
        // value属性（デフォルト）またはname属性から値を取得
        val annotationValue = annotation?.arguments?.let { args ->
            // デフォルト引数（value）を探す
            args.firstOrNull { it.name?.asString() == null || it.name?.asString() == "value" }?.value?.toString()?.removeSurrounding("\"")
                ?: args.firstOrNull { it.name?.asString() == "name" }?.value?.toString()?.removeSurrounding("\"")
        }
        
        // アノテーション値が空文字列の場合はパラメータ名を使用
        return when {
            annotationValue != null && annotationValue.isNotEmpty() -> annotationValue
            else -> parameter.name?.asString() ?: "unknown"
        }
    }
}