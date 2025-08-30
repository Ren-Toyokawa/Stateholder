package io.github.rentoyokawa.stateholder.ksp

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSValueParameter
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.validate
import io.github.rentoyokawa.stateholder.annotations.StateHolder
import io.github.rentoyokawa.stateholder.annotations.SharedState
import io.github.rentoyokawa.stateholder.annotations.InjectedParam
import io.github.rentoyokawa.stateholder.annotations.InjectStateHolder

/**
 * StateHolderアノテーション処理のためのKSPプロセッサー
 * 
 * このプロセッサーは以下のアノテーションを検出し、基本的なログ出力を行います：
 * - @StateHolder: StateHolderクラスの検出
 * - @SharedState: 共有状態プロパティの検出  
 * - @InjectedParam: 注入パラメータの検出
 * - @InjectStateHolder: StateHolder注入の検出
 */
class StateHolderProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
    private val options: Map<String, String> = emptyMap()
) : SymbolProcessor {
    
    private val stateHolderModuleGenerator = StateHolderModuleGenerator(codeGenerator, logger)
    private var koinModuleGenerated = false
    
    // モジュール名を生成（プロジェクト名またはオプションから取得）
    private val moduleName: String by lazy {
        // KSPオプションからモジュール名を取得、なければデフォルト値を使用
        val projectName = options["project.name"] ?: ""
        val moduleSuffix = options["stateholder.module.suffix"] ?: ""
        
        when {
            moduleSuffix.isNotBlank() -> "stateHolderModule_$moduleSuffix"
            projectName.isNotBlank() -> {
                // プロジェクト名から有効な識別子を生成
                val sanitized = projectName.replace(Regex("[^a-zA-Z0-9_]"), "_")
                    .replace(Regex("^[0-9]"), "_")
                    .lowercase()
                "stateHolderModule_$sanitized"
            }
            else -> {
                // フォールバック: ファイルのハッシュコードを使用して一意性を確保
                val uniqueId = System.currentTimeMillis().toString().takeLast(6)
                "stateHolderModule_$uniqueId"
            }
        }
    }

    companion object {
        private val STATE_HOLDER_ANNOTATION = StateHolder::class.qualifiedName!!
        private val SHARED_STATE_ANNOTATION = SharedState::class.qualifiedName!!
        private val INJECTED_PARAM_ANNOTATION = InjectedParam::class.qualifiedName!!
        private val INJECT_STATE_HOLDER_ANNOTATION = InjectStateHolder::class.qualifiedName!!
        private val VIEW_MODEL_CLASS = "androidx.lifecycle.ViewModel"
    }

    override fun process(resolver: Resolver): List<KSAnnotated> {
        logger.info("[ksp] StateHolderProcessor started - Files count: ${resolver.getAllFiles().count()}")
        
        val deferredSymbols = mutableListOf<KSAnnotated>()
        
        try {
            // @StateHolderアノテーションが付いたクラスからViewModelとのマッピングを生成
            processStateHolderClasses(resolver, deferredSymbols)
            
            // その他のアノテーション処理（ログ目的）
            processSharedStateProperties(resolver, deferredSymbols)
            processInjectedParams(resolver, deferredSymbols)
            
        } catch (e: Exception) {
            logger.error("StateHolderProcessor処理中にエラーが発生しました: ${e.message}", null)
            logger.exception(e)
        }
        
        // Koinモジュールを生成（1回のみ）
        if (!koinModuleGenerated) {
            generateKoinModule(resolver)
            koinModuleGenerated = true
        }
        
        logger.info("[ksp] StateHolderProcessor completed. Deferred symbols: ${deferredSymbols.size}")
        
        return deferredSymbols
    }


    /**
     * @SharedStateアノテーションが付いたプロパティを処理
     */
    private fun processSharedStateProperties(resolver: Resolver, deferredSymbols: MutableList<KSAnnotated>) {
        val sharedStateSymbols = resolver.getSymbolsWithAnnotation(SHARED_STATE_ANNOTATION)
        
        sharedStateSymbols.forEach { symbol ->
            if (!symbol.validate()) {
                deferredSymbols.add(symbol)
                return@forEach
            }
            
            when (symbol) {
                is KSPropertyDeclaration -> {
                    logger.info("[ksp] @SharedState検出: ${symbol.qualifiedName?.asString()}")
                    
                    // プロパティの基本情報をログ出力
                    logPropertyInfo(symbol)
                }
                else -> {
                    logger.warn("[ksp] @SharedStateアノテーションがプロパティ以外に付与されています: ${symbol.javaClass.simpleName}")
                }
            }
        }
    }

    /**
     * @InjectedParamアノテーションが付いたパラメータを処理
     */
    private fun processInjectedParams(resolver: Resolver, deferredSymbols: MutableList<KSAnnotated>) {
        val injectedParamSymbols = resolver.getSymbolsWithAnnotation(INJECTED_PARAM_ANNOTATION)
        
        injectedParamSymbols.forEach { symbol ->
            if (!symbol.validate()) {
                deferredSymbols.add(symbol)
                return@forEach
            }
            
            when (symbol) {
                is KSValueParameter -> {
                    logger.info("@InjectedParam検出: ${symbol.name?.asString()}")
                    
                    // パラメータの基本情報をログ出力
                    logParameterInfo(symbol)
                    
                    // アノテーションの値を取得
                    val keyValue = getInjectedParamKey(symbol)
                    if (keyValue != null) {
                        logger.info("  key: $keyValue")
                    } else {
                        logger.info("  key: (デフォルト: パラメータ名を使用)")
                    }
                }
                else -> {
                    logger.warn("@InjectedParamアノテーションがパラメータ以外に付与されています: ${symbol.javaClass.simpleName}")
                }
            }
        }
    }

    /**
     * @StateHolderアノテーションが付いたクラスを処理してViewModelとのマッピングを生成
     */
    private fun processStateHolderClasses(resolver: Resolver, deferredSymbols: MutableList<KSAnnotated>) {
        logger.info("[ksp] Processing @StateHolder classes")
        val stateHolderSymbols = resolver.getSymbolsWithAnnotation(STATE_HOLDER_ANNOTATION)
        
        stateHolderSymbols.forEach { symbol ->
            if (!symbol.validate()) {
                deferredSymbols.add(symbol)
                return@forEach
            }
            
            when (symbol) {
                is KSClassDeclaration -> {
                    logger.info("[ksp] @StateHolder検出: ${symbol.qualifiedName?.asString()}")
                    
                    // クラス情報をログ出力
                    logClassInfo(symbol)
                    
                    // injectable = true かどうかチェック
                    val isInjectable = isInjectableStateHolder(symbol)
                    if (isInjectable) {
                        logger.info("[ksp]   注入可能なStateHolder: ${symbol.simpleName.asString()}")
                    }
                }
                else -> {
                    logger.warn("[ksp] @StateHolderアノテーションがクラス以外に付与されています: ${symbol.javaClass.simpleName}")
                }
            }
        }
    }


    /**
     * クラスの基本情報をログ出力
     */
    private fun logClassInfo(classDeclaration: KSClassDeclaration) {
        logger.info("  クラス名: ${classDeclaration.simpleName.asString()}")
        logger.info("  完全修飾名: ${classDeclaration.qualifiedName?.asString()}")
        logger.info("  パッケージ: ${classDeclaration.packageName.asString()}")
        logger.info("  クラス種別: ${classDeclaration.classKind}")
        
        // スーパータイプの情報
        val superTypes = classDeclaration.superTypes.toList()
        if (superTypes.isNotEmpty()) {
            logger.info("  スーパータイプ数: ${superTypes.size}")
            superTypes.forEach { superType ->
                logger.info("    - ${superType.resolve().declaration.qualifiedName?.asString()}")
            }
        }
    }

    /**
     * プロパティの基本情報をログ出力
     */
    private fun logPropertyInfo(propertyDeclaration: KSPropertyDeclaration) {
        logger.info("  プロパティ名: ${propertyDeclaration.simpleName.asString()}")
        logger.info("  型: ${propertyDeclaration.type.resolve().declaration.qualifiedName?.asString()}")
        logger.info("  可変性: ${if (propertyDeclaration.isMutable) "mutable" else "immutable"}")
        logger.info("  所属クラス: ${propertyDeclaration.parentDeclaration?.qualifiedName?.asString()}")
    }

    /**
     * パラメータの基本情報をログ出力
     */
    private fun logParameterInfo(parameter: KSValueParameter) {
        logger.info("  パラメータ名: ${parameter.name?.asString()}")
        logger.info("  型: ${parameter.type.resolve().declaration.qualifiedName?.asString()}")
        logger.info("  デフォルト値: ${if (parameter.hasDefault) "あり" else "なし"}")
        
        // 所属する関数の情報
        val parentFunction = parameter.parent as? KSFunctionDeclaration
        parentFunction?.let {
            logger.info("  所属関数: ${it.simpleName.asString()}")
        }
    }

    /**
     * @InjectedParamアノテーションのkey値を取得
     */
    private fun getInjectedParamKey(parameter: KSValueParameter): String? {
        val annotation = parameter.annotations
            .firstOrNull { it.shortName.asString() == "InjectedParam" }
        
        // @InjectedParamアノテーションの値を取得
        // value属性（デフォルト）またはname属性、またはkey属性から値を取得
        return annotation?.arguments?.let { args ->
            // デフォルト引数（value）を探す
            args.firstOrNull { it.name?.asString() == null || it.name?.asString() == "value" }?.value?.toString()?.removeSurrounding("\"")
                ?: args.firstOrNull { it.name?.asString() == "name" }?.value?.toString()?.removeSurrounding("\"")
                ?: args.firstOrNull { it.name?.asString() == "key" }?.value?.toString()?.removeSurrounding("\"")
        }
    }


    /**
     * クラスがViewModelを継承しているかどうかを判定
     */
    private fun isViewModelClass(classDeclaration: KSClassDeclaration): Boolean {
        return classDeclaration.superTypes.any { superType ->
            superType.resolve().declaration.qualifiedName?.asString() == VIEW_MODEL_CLASS
        }
    }

    /**
     * クラスのコンストラクタから@InjectStateHolderパラメータを取得
     */
    private fun getInjectStateHolderParameters(classDeclaration: KSClassDeclaration): List<KSValueParameter> {
        return classDeclaration.primaryConstructor?.parameters?.filter { param ->
            param.annotations.any { annotation ->
                annotation.shortName.asString() == "InjectStateHolder"
            }
        } ?: emptyList()
    }

    /**
     * クラスから@SharedStateアノテーションが付いたフィールドを取得
     */
    private fun getSharedStateFields(classDeclaration: KSClassDeclaration): List<KSPropertyDeclaration> {
        return classDeclaration.getAllProperties().filter { property ->
            property.annotations.any { annotation ->
                annotation.shortName.asString() == "SharedState"
            }
        }.toList()
    }

    /**
     * クラスがStateHolderを継承しているかどうかを判定
     */
    private fun isStateHolderClass(classDeclaration: KSClassDeclaration): Boolean {
        return classDeclaration.superTypes.any { superType ->
            val declaration = superType.resolve().declaration
            declaration.qualifiedName?.asString()?.startsWith("io.github.rentoyokawa.stateholder.StateHolder") == true
        }
    }

    /**
     * パラメータからViewModelクラスを取得
     */
    private fun getViewModelFromParameter(parameter: KSValueParameter): KSClassDeclaration? {
        // パラメータの親（関数）から、その親（クラス）を取得
        val function = parameter.parent as? KSFunctionDeclaration
        return function?.parentDeclaration as? KSClassDeclaration
    }

    /**
     * StateHolderアノテーションのinjectable属性をチェック
     */
    private fun isInjectableStateHolder(classDeclaration: KSClassDeclaration): Boolean {
        val annotation = classDeclaration.annotations
            .firstOrNull { 
                val annotationName = it.annotationType.resolve().declaration.qualifiedName?.asString()
                logger.info("      アノテーション比較: ${it.shortName.asString()} vs StateHolder, QualifiedName: $annotationName")
                annotationName == STATE_HOLDER_ANNOTATION
            }
        
        if (annotation == null) {
            logger.info("      StateHolderアノテーションが見つかりません")
            return false
        }
        
        // injectable パラメータの値を取得（デフォルトはtrue）
        val injectableArg = annotation.arguments
            .firstOrNull { it.name?.asString() == "injectable" }
        
        val result = injectableArg?.value as? Boolean ?: true
        logger.info("      injectable引数: ${injectableArg?.value}, 結果: $result")
        return result
    }


    /**
     * Koinモジュールを生成
     */
    private fun generateKoinModule(resolver: Resolver) {
        logger.info("Koinモジュール生成開始")
        
        // すべての@StateHolderクラスを収集
        val stateHolderClasses = mutableListOf<KSClassDeclaration>()
        val viewModelStateHolderMappings = mutableMapOf<String, MutableList<Pair<KSClassDeclaration, List<KSPropertyDeclaration>>>>() // StateHolder -> [(ViewModel, SharedStates)]
        
        val stateHolderSymbols = resolver.getSymbolsWithAnnotation(STATE_HOLDER_ANNOTATION)
        logger.info("  @StateHolderアノテーション検索結果: ${stateHolderSymbols.count()} 件")
        
        stateHolderSymbols.forEach { symbol ->
            logger.info("  シンボル検出: ${symbol.javaClass.simpleName}, validate=${symbol.validate()}")
            if (symbol is KSClassDeclaration) {
                logger.info("    クラス名: ${symbol.simpleName.asString()}, QualifiedName: ${symbol.qualifiedName?.asString()}")
                val isInjectable = isInjectableStateHolder(symbol)
                logger.info("    injectable: $isInjectable")
                if (isInjectable) {
                    stateHolderClasses.add(symbol)
                    logger.info("  StateHolder収集: ${symbol.simpleName.asString()}")
                    
                    // 使用されているViewModelを検索
                    val stateHolderName = symbol.qualifiedName?.asString() ?: return@forEach
                    viewModelStateHolderMappings[stateHolderName] = mutableListOf()
                }
            }
        }
        
        logger.info("  StateHolderクラス数: ${stateHolderClasses.size}")
        
        // ViewModelを検索してマッピングを構築
        if (stateHolderClasses.isNotEmpty()) {
            resolver.getAllFiles().forEach { file ->
                file.declarations.forEach { declaration ->
                    if (declaration is KSClassDeclaration && isViewModelClass(declaration)) {
                        val sharedStateFields = getSharedStateFields(declaration)
                        
                        // このViewModelで使用されているStateHolderを探す
                        stateHolderClasses.forEach { stateHolder ->
                            if (isStateHolderUsedInViewModel(declaration, stateHolder)) {
                                val stateHolderName = stateHolder.qualifiedName?.asString() ?: return@forEach
                                viewModelStateHolderMappings[stateHolderName]?.add(declaration to sharedStateFields)
                                logger.info("    マッピング追加: ${declaration.simpleName.asString()} -> ${stateHolder.simpleName.asString()}")
                            }
                        }
                    }
                }
            }
            
            // StateHolderクラスのパッケージ名を取得（最初のStateHolderのパッケージを基準にする）
            val basePackageName = if (stateHolderClasses.isNotEmpty()) {
                val firstStateHolder = stateHolderClasses.first()
                val packageName = firstStateHolder.packageName.asString()
                // パッケージ名の末尾に.generatedを追加
                "$packageName.generated"
            } else {
                "io.github.rentoyokawa.stateholder.koin.generated"
            }
            
            logger.info("  マッピング数: ${viewModelStateHolderMappings.size}")
            logger.info("  Koinモジュール生成を開始します（パッケージ: $basePackageName, モジュール名: $moduleName）")
            
            // Koinモジュールを生成（改善されたファクトリー定義を含む）
            stateHolderModuleGenerator.generateStateHolderModuleWithMappings(
                stateHolderClasses = stateHolderClasses, 
                viewModelMappings = viewModelStateHolderMappings,
                packageName = basePackageName,
                moduleName = moduleName
            )
        } else {
            logger.info("  StateHolderクラスが見つからないため、Koinモジュール生成をスキップします")
        }
        
        logger.info("Koinモジュール生成完了")
    }
    
    /**
     * ViewModelでStateHolderが使用されているかチェック
     */
    private fun isStateHolderUsedInViewModel(
        viewModelClass: KSClassDeclaration,
        stateHolderClass: KSClassDeclaration
    ): Boolean {
        val stateHolderName = stateHolderClass.qualifiedName?.asString() ?: return false
        
        return viewModelClass.getAllProperties().any { property ->
            val propertyType = property.type.resolve()
            val propertyTypeName = propertyType.declaration.qualifiedName?.asString()
            
            // 直接の型一致またはStateHolderDelegateの型引数として使用されているかチェック
            if (propertyTypeName == stateHolderName) {
                return@any true
            }
            
            // StateHolderDelegateの型引数をチェック
            propertyType.arguments.firstOrNull()?.type?.resolve()?.let { typeArg ->
                return@any typeArg.declaration.qualifiedName?.asString() == stateHolderName
            }
            
            false
        }
    }

}