package io.github.rentoyokawa.stateholder.ksp.generator

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeName
import io.github.rentoyokawa.stateholder.ksp.domain.models.HolderModel
import io.github.rentoyokawa.stateholder.ksp.domain.models.ParamKind
import io.github.rentoyokawa.stateholder.ksp.domain.models.ViewModelModel

/**
 * 検出済みの [ViewModelModel] 群から Koin モジュールを生成する。
 *
 * 1 VM につき 1 つの `factory<VM> { }` 定義を出力する。
 *
 * DSL 選択（`viewModel {}` ではなく `factory {}`）については [[STA-14]] intent 参照:
 * `stateholder-processor-koin-test` は koin-core 3.5.6 のみの純 JVM モジュールで、
 * `viewModel {}` DSL（org.koin.androidx.viewmodel）は android 系アーティファクト由来のため
 * JVM では解決できない。受入基準は「生成・コンパイル成功」のため `factory {}` を採用し、
 * 実際の `viewModel {}` DSL 適用（Android 実証）は後続 [[STA-19]] に委ねる。
 *
 * holder は Koin 定義として個別登録せず、VM 構築ブロック内で具象型インライン構築する
 * （ジェネリクス型消去による `Store<*,*,*>` 系の衝突回避、チケット指定）。
 */
class KoinFactoryGenerator {

    companion object {
        private val MODULE_CLASS = ClassName("org.koin.core.module", "Module")
        private val MODULE_FUNCTION = MemberName("org.koin.dsl", "module")
        private val COROUTINE_SCOPE = ClassName("kotlinx.coroutines", "CoroutineScope")
        private val SUPERVISOR_JOB = ClassName("kotlinx.coroutines", "SupervisorJob")
        private val DISPATCHERS = ClassName("kotlinx.coroutines", "Dispatchers")
        private val SHARED_STATE = ClassName("io.github.rentoyokawa.stateholder.core", "SharedState")
    }

    /**
     * [viewModels]（既に qualifiedName でソート済みであること）から Koin モジュールを含む [FileSpec] を作る。
     */
    fun generateModuleFileSpec(
        viewModels: List<ViewModelModel>,
        packageName: String,
        fileName: String,
        moduleName: String,
    ): FileSpec {
        val moduleProperty = PropertySpec.builder(moduleName, MODULE_CLASS)
            .initializer(buildModuleInitializer(viewModels))
            .build()

        return FileSpec.builder(packageName, fileName)
            .addFileComment("@file:Suppress(\"UNUSED\")")
            .addProperty(moduleProperty)
            .build()
    }

    private fun buildModuleInitializer(viewModels: List<ViewModelModel>): CodeBlock {
        val builder = CodeBlock.builder()
        builder.add("%M {\n", MODULE_FUNCTION)
        builder.indent()
        viewModels.forEach { vm -> builder.add(buildFactoryBlock(vm)) }
        builder.unindent()
        builder.add("}")
        return builder.build()
    }

    private fun buildFactoryBlock(vm: ViewModelModel): CodeBlock {
        val vmClassName = ClassName(vm.packageName, vm.simpleName)
        val builder = CodeBlock.builder()
        builder.add("factory<%T> {\n", vmClassName)
        builder.indent()

        val needsScope = vm.holderParams.any { hp -> hp.holder.params.any { it.kind is ParamKind.Scope } }
        if (needsScope) {
            builder.add(
                "val scope = %T(%T() + %T.Main.immediate)\n",
                COROUTINE_SCOPE,
                SUPERVISOR_JOB,
                DISPATCHERS,
            )
        }

        val sharedGroups = collectSharedGroups(vm)
        sharedGroups.forEach { group ->
            builder.add("val %L = %T<%T>(null)\n", group.varName, SHARED_STATE, group.typeName)
        }

        builder.add("%T(\n", vmClassName)
        builder.indent()
        vm.holderParams.forEachIndexed { index, holderParam ->
            builder.add("%L = %L", holderParam.name, buildHolderConstruction(holderParam.holder, sharedGroups))
            if (index != vm.holderParams.lastIndex) builder.add(",")
            builder.add("\n")
        }
        builder.unindent()
        builder.add(")\n")

        builder.unindent()
        builder.add("}\n")
        return builder.build()
    }

    private fun buildHolderConstruction(holder: HolderModel, sharedGroups: List<SharedGroup>): CodeBlock {
        val holderClassName = ClassName(holder.packageName, holder.simpleName)
        val builder = CodeBlock.builder()
        builder.add("%T(", holderClassName)
        holder.params.forEachIndexed { index, param ->
            val valueLiteral = when (val kind = param.kind) {
                is ParamKind.Shared -> sharedGroups.first { it.typeKey == kind.typeArg.toString() }.varName
                ParamKind.Scope -> "scope"
                ParamKind.Resolved -> "get()"
            }
            builder.add("%L = %L", param.name, valueLiteral)
            if (index != holder.params.lastIndex) builder.add(", ")
        }
        builder.add(")")
        return builder.build()
    }

    /**
     * VM 内の全 holder 引数を SharedState の型ごとにグルーピングし、共有ローカル変数名を割り当てる。
     *
     * 変数名は「その型を最初に要求した holder 引数の名前」を採用する（[[STA-14]] plan の生成物イメージに合わせ、
     * 可読性を優先）。holder・VM は既に qualifiedName でソート済みのため、この割当は入力順序に依らず決定的。
     */
    private fun collectSharedGroups(vm: ViewModelModel): List<SharedGroup> {
        val sharedParamsInOrder = vm.holderParams
            .flatMap { it.holder.params }
            .mapNotNull { param -> (param.kind as? ParamKind.Shared)?.let { param.name to it } }

        val groupedByTypeKey = sharedParamsInOrder.groupBy { it.second.typeArg.toString() }

        val usedNames = mutableSetOf<String>()
        return groupedByTypeKey.toSortedMap().map { (typeKey, entries) ->
            val baseName = entries.first().first
            var varName = baseName
            var suffix = 2
            while (!usedNames.add(varName)) {
                varName = "$baseName$suffix"
                suffix++
            }
            SharedGroup(varName = varName, typeName = entries.first().second.typeArg, typeKey = typeKey)
        }
    }

    private data class SharedGroup(val varName: String, val typeName: TypeName, val typeKey: String)
}
