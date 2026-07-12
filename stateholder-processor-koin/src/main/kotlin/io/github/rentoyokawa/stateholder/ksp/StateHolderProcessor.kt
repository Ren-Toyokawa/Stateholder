package io.github.rentoyokawa.stateholder.ksp

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSFile
import io.github.rentoyokawa.stateholder.ksp.core.HolderDetector
import io.github.rentoyokawa.stateholder.ksp.core.HolderValidator
import io.github.rentoyokawa.stateholder.ksp.core.ViewModelDetector
import io.github.rentoyokawa.stateholder.ksp.domain.models.ViewModelModel
import io.github.rentoyokawa.stateholder.ksp.generator.KoinFactoryGenerator

/**
 * `@StateHolder` アノテーション処理のための KSP プロセッサー（新設計向け刷新版）。
 *
 * `@StateHolder` 付き具象 holder ＋ それをコンストラクタで受け取る ViewModel を検出し、
 * VM ごとに Koin `factory { }` 定義を1ファイルへ生成する。生成モデルの詳細は
 * [HolderDetector] / [ViewModelDetector] / [KoinFactoryGenerator] を参照。
 */
class StateHolderProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
    private val options: Map<String, String> = emptyMap(),
) : SymbolProcessor {

    private var moduleGenerated = false

    // 生成Koinモジュールの出力先パッケージ名（KSPオプションから取得。未指定ならnull）
    private val modulePackage: String? by lazy {
        options["stateholder.module.package"]?.takeIf { it.isNotBlank() }
    }

    // モジュール名を生成（プロジェクト名またはオプションから取得）
    private val moduleName: String by lazy {
        val projectName = options["project.name"] ?: ""
        val moduleSuffix = options["stateholder.module.suffix"] ?: ""

        when {
            moduleSuffix.isNotBlank() -> "stateHolderModule_$moduleSuffix"
            projectName.isNotBlank() -> {
                val sanitized = projectName.replace(Regex("[^a-zA-Z0-9_]"), "_")
                    .replace(Regex("^[0-9]"), "_")
                    .lowercase()
                "stateHolderModule_$sanitized"
            }
            else -> {
                val uniqueId = System.currentTimeMillis().toString().takeLast(6)
                "stateHolderModule_$uniqueId"
            }
        }
    }

    override fun process(resolver: Resolver): List<KSAnnotated> {
        logger.info("[ksp] StateHolderProcessor started - Files count: ${resolver.getAllFiles().count()}")

        val holders = HolderDetector(resolver, logger).detectHolders()
        logger.info("[ksp] @StateHolder holder 検出数: ${holders.size}")

        if (holders.isEmpty()) {
            logger.info("[ksp] @StateHolder holder が見つからないため生成をスキップします")
            return emptyList()
        }

        val hasValidationError = HolderValidator(logger).validate(holders)
        if (hasValidationError) {
            logger.info("[ksp] holder のバリデーションエラーのため生成をスキップします")
            return emptyList()
        }

        val viewModels = ViewModelDetector(resolver).detectViewModels(holders)
        logger.info("[ksp] holder をコンストラクタで受け取る ViewModel 検出数: ${viewModels.size}")

        if (viewModels.isEmpty()) {
            logger.info("[ksp] 対象 ViewModel が見つからないため生成をスキップします")
            return emptyList()
        }

        if (!moduleGenerated) {
            generateModule(viewModels)
            moduleGenerated = true
        }

        logger.info("[ksp] StateHolderProcessor completed")
        return emptyList()
    }

    private fun generateModule(viewModels: List<ViewModelModel>) {
        val explicitModulePackage = modulePackage
        val basePackageName = when {
            explicitModulePackage != null -> "$explicitModulePackage.generated"
            // 未指定時: distinct パッケージ集合を辞書順ソートした先頭を使う。
            // getSymbolsWithAnnotation()/getAllFiles() の列挙順は非決定的なため first() は使わない（[[STA-11]] 教訓）
            else -> viewModels.map { it.packageName }.distinct().minOrNull()?.let { "$it.generated" }
                ?: "io.github.rentoyokawa.stateholder.koin.generated"
        }

        val fileName = "StateHolderModule_Generated"
        logger.info("[ksp] Koinモジュール生成開始（パッケージ: $basePackageName, モジュール名: $moduleName, VM数: ${viewModels.size}）")

        val fileSpec = KoinFactoryGenerator().generateModuleFileSpec(
            viewModels = viewModels,
            packageName = basePackageName,
            fileName = fileName,
            moduleName = moduleName,
        )

        val dependencyFiles: Array<KSFile> = viewModels
            .flatMap { vm -> listOfNotNull(vm.containingFile) + vm.holderParams.mapNotNull { it.holder.containingFile } }
            .distinct()
            .toTypedArray()

        val file = codeGenerator.createNewFile(
            dependencies = Dependencies(false, *dependencyFiles),
            packageName = basePackageName,
            fileName = fileName,
        )

        file.use { outputStream ->
            outputStream.write(fileSpec.toString().toByteArray())
        }

        logger.info("[ksp] Koinモジュール生成完了: $basePackageName.$fileName")
    }
}
