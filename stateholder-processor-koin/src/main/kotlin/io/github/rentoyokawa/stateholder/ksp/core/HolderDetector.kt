package io.github.rentoyokawa.stateholder.ksp.core

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSValueParameter
import com.google.devtools.ksp.validate
import com.squareup.kotlinpoet.ksp.toTypeName
import io.github.rentoyokawa.stateholder.annotations.StateHolder
import io.github.rentoyokawa.stateholder.ksp.domain.models.HolderModel
import io.github.rentoyokawa.stateholder.ksp.domain.models.HolderParam
import io.github.rentoyokawa.stateholder.ksp.domain.models.ParamKind

/**
 * `@StateHolder` が付いた具象 holder クラスを検出し、[HolderModel] へ変換するアナライザー。
 *
 * コンストラクタ引数の型が [SHARED_STATE_QUALIFIED_NAME] / [COROUTINE_SCOPE_QUALIFIED_NAME] かどうかで
 * 機械的に分類する（[[STA-13]] の設計: 追加アノテーション不要・型が自己記述的）。
 */
class HolderDetector(
    private val resolver: Resolver,
    private val logger: KSPLogger,
) {
    companion object {
        private val STATE_HOLDER_ANNOTATION = StateHolder::class.qualifiedName!!
        const val SHARED_STATE_QUALIFIED_NAME = "io.github.rentoyokawa.stateholder.core.SharedState"
        const val COROUTINE_SCOPE_QUALIFIED_NAME = "kotlinx.coroutines.CoroutineScope"
    }

    /**
     * `@StateHolder` クラスを検出して [HolderModel] のリストを返す（qualifiedName の辞書順）。
     *
     * 未検証（[com.google.devtools.ksp.symbol.KSAnnotated.validate]）のシンボルは今回のラウンドでは
     * 除外する（次ラウンドでの解決を待つ複雑な再試行は本実装のスコープ外）。
     */
    fun detectHolders(): List<HolderModel> {
        val symbols = resolver.getSymbolsWithAnnotation(STATE_HOLDER_ANNOTATION)

        return symbols
            .filterIsInstance<KSClassDeclaration>()
            .filter { it.validate() }
            .mapNotNull { toHolderModel(it) }
            .toList()
            .sortedBy { it.qualifiedName }
    }

    private fun toHolderModel(declaration: KSClassDeclaration): HolderModel? {
        val qualifiedName = declaration.qualifiedName?.asString()
        if (qualifiedName == null) {
            logger.warn("[ksp] @StateHolder クラスの qualifiedName を解決できません: ${declaration.simpleName.asString()}")
            return null
        }

        val primaryConstructor = declaration.primaryConstructor
        val params = primaryConstructor?.parameters.orEmpty().map { toHolderParam(it) }

        return HolderModel(
            qualifiedName = qualifiedName,
            simpleName = declaration.simpleName.asString(),
            packageName = declaration.packageName.asString(),
            params = params,
            containingFile = declaration.containingFile,
        )
    }

    private fun toHolderParam(parameter: KSValueParameter): HolderParam {
        val name = parameter.name?.asString() ?: "unknown"
        val type = parameter.type.resolve()
        val kind = classifyParam(type)
        return HolderParam(name = name, kind = kind)
    }

    private fun classifyParam(type: KSType): ParamKind {
        val qualifiedName = type.declaration.qualifiedName?.asString()
        return when (qualifiedName) {
            SHARED_STATE_QUALIFIED_NAME -> {
                val typeArg = type.arguments.firstOrNull()?.type?.resolve()
                if (typeArg == null) {
                    ParamKind.Resolved
                } else {
                    ParamKind.Shared(typeArg.toTypeName())
                }
            }
            COROUTINE_SCOPE_QUALIFIED_NAME -> ParamKind.Scope
            else -> ParamKind.Resolved
        }
    }
}
