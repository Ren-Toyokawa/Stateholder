package io.github.rentoyokawa.stateholder.ksp.adapter

import com.google.devtools.ksp.symbol.*
import io.github.rentoyokawa.stateholder.ksp.domain.models.ParameterModel
import io.github.rentoyokawa.stateholder.ksp.domain.models.SharedStateModel
import io.github.rentoyokawa.stateholder.ksp.domain.models.StateHolderModel

/**
 * KSP APIのシンボルからドメインモデルへの変換を行うアダプター
 * KSP APIへの依存を局所化し、テスト可能性を向上させる
 * 
 * 使用例:
 * ```
 * val converter = KSPModelConverter()
 * val stateHolderModel = converter.convertClass(ksClassDeclaration)
 * ```
 */
class KSPModelConverter {
    
    private companion object {
        const val STATE_HOLDER_ANNOTATION = "StateHolder"
        const val INJECTED_PARAM_ANNOTATION = "InjectedParam"
        const val SHARED_STATE_ANNOTATION = "SharedState"
        const val INJECTABLE_ARGUMENT = "injectable"
        const val KEY_ARGUMENT = "key"
    }
    
    /**
     * KSClassDeclarationからStateHolderModelへの変換
     */
    fun convertClass(ksClass: KSClassDeclaration): StateHolderModel {
        return StateHolderModel(
            className = ksClass.simpleName.asString(),
            packageName = ksClass.packageName.asString(),
            constructorParams = extractParameters(ksClass),
            sharedStates = extractSharedStates(ksClass),
            isInjectable = extractInjectableFlag(ksClass)
        )
    }
    
    /**
     * 単一パラメータの変換
     * KSValueParameterからParameterModelへの変換を行う
     * 
     * @param ksParam 変換対象のKSValueParameter
     * @return 変換されたParameterModel
     */
    fun convertParameter(ksParam: KSValueParameter): ParameterModel {
        val injectedAnnotation = ksParam.annotations.find { annotation ->
            annotation.shortName.asString() == INJECTED_PARAM_ANNOTATION
        }
        
        val isInjected = injectedAnnotation != null
        val injectionKey = extractInjectionKey(injectedAnnotation, ksParam)
        
        return ParameterModel(
            name = ksParam.name?.asString() ?: "",
            typeName = ksParam.type.resolve().toString(),
            isInjected = isInjected,
            injectionKey = injectionKey
        )
    }
    
    /**
     * 単一プロパティの変換
     * KSPropertyDeclarationからSharedStateModelへの変換を行う
     * @SharedStateアノテーションが付いている場合のみ変換を行う
     * 
     * @param ksProp 変換対象のKSPropertyDeclaration
     * @return @SharedStateアノテーションが付いている場合はSharedStateModel、そうでない場合はnull
     */
    fun convertProperty(ksProp: KSPropertyDeclaration): SharedStateModel? {
        val hasSharedState = ksProp.annotations
            .any { it.shortName.asString() == SHARED_STATE_ANNOTATION }
        
        return if (hasSharedState) {
            SharedStateModel(
                propertyName = ksProp.simpleName.asString(),
                typeName = ksProp.type.resolve().declaration.simpleName.asString(),
                isMutable = ksProp.isMutable
            )
        } else null
    }
    
    /**
     * Injection keyの抽出
     * アノテーションからkeyパラメータを取得し、存在しない場合はパラメータ名を使用
     */
    private fun extractInjectionKey(injectedAnnotation: KSAnnotation?, ksParam: KSValueParameter): String? {
        if (injectedAnnotation == null) return null
        
        val keyArgument = injectedAnnotation.arguments.find { it.name?.asString() == KEY_ARGUMENT }
        val keyValue = keyArgument?.value
        
        return when (keyValue) {
            is String -> keyValue
            null -> ksParam.name?.asString()
            else -> ksParam.name?.asString() // String以外の値の場合はパラメータ名を使用
        }
    }
    
    /**
     * コンストラクタパラメータの抽出
     */
    private fun extractParameters(ksClass: KSClassDeclaration): List<ParameterModel> {
        val constructor = ksClass.primaryConstructor ?: return emptyList()
        return constructor.parameters.map { param -> convertParameter(param) }
    }
    
    /**
     * SharedStateプロパティの抽出
     */
    private fun extractSharedStates(ksClass: KSClassDeclaration): List<SharedStateModel> {
        return ksClass.getAllProperties()
            .filter { property ->
                property.annotations.any { annotation ->
                    annotation.shortName.asString() == SHARED_STATE_ANNOTATION
                }
            }
            .map { property ->
                SharedStateModel(
                    propertyName = property.simpleName.asString(),
                    typeName = property.type.resolve().toString(),
                    isMutable = property.isMutable
                )
            }
            .toList() // Sequenceから明示的にListに変換する必要があるため、実際には必要
    }
    /**
     * injectable フラグの抽出
     */
    private fun extractInjectableFlag(ksClass: KSClassDeclaration): Boolean {
        val stateHolderAnnotation = ksClass.annotations.find { annotation ->
            annotation.shortName.asString() == STATE_HOLDER_ANNOTATION
        }
        
        val injectableArgument = stateHolderAnnotation?.arguments
            ?.find { it.name?.asString() == INJECTABLE_ARGUMENT }
        
        // より明確な型チェック
        return when (val value = injectableArgument?.value) {
            is Boolean -> value
            else -> false // Boolean以外の値やnullの場合はfalse
        }
    }
}