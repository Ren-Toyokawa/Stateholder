@file:OptIn(org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi::class)

package io.github.rentoyokawa.stateholder.ksp

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import com.tschuchort.compiletesting.kspArgs
import com.tschuchort.compiletesting.kspSourcesDir
import com.tschuchort.compiletesting.symbolProcessorProviders
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 生成Koinモジュールの出力先パッケージ決定ロジックの決定性を検証するテスト。
 *
 * 新設計（[[STA-14]]）では基準は「ViewModel のパッケージ」の distinct 集合の辞書順ソート先頭になる。
 * `getSymbolsWithAnnotation()` / `getAllFiles()` の列挙順は非決定的なため、
 * [ViewModelDetector] や本プロセッサーが `first()` 等の未ソート列挙依存を再発させていないことを保証する
 * （[[STA-11]] のフレーク再発防止）。
 */
class DeterministicPackageTest {

    /** VM が複数パッケージに分散した入力一式。辞書順で "com.example.alpha" が先頭になるようにしている。 */
    private fun multiPackageSources(): List<SourceFile> = listOf(
        SourceFile.kotlin(
            "Repo.kt",
            """
            package com.example.repo

            class FakeRepository
            """.trimIndent(),
        ),
        SourceFile.kotlin(
            "ZetaHolder.kt",
            """
            package com.example.holder

            import io.github.rentoyokawa.stateholder.annotations.StateHolder
            import com.example.repo.FakeRepository

            @StateHolder
            class ZetaStateHolder(val repository: FakeRepository)
            """.trimIndent(),
        ),
        SourceFile.kotlin(
            "ZetaViewModel.kt",
            """
            package com.example.zeta

            import androidx.lifecycle.ViewModel
            import com.example.holder.ZetaStateHolder

            class ZetaViewModel(val holder: ZetaStateHolder) : ViewModel()
            """.trimIndent(),
        ),
        SourceFile.kotlin(
            "AlphaViewModel.kt",
            """
            package com.example.alpha

            import androidx.lifecycle.ViewModel
            import com.example.holder.ZetaStateHolder

            class AlphaViewModel(val holder: ZetaStateHolder) : ViewModel()
            """.trimIndent(),
        ),
    )

    private fun singlePackageSources(): List<SourceFile> = listOf(
        SourceFile.kotlin(
            "Repo.kt",
            """
            package com.example.single

            class FakeRepository
            """.trimIndent(),
        ),
        SourceFile.kotlin(
            "SingleHolder.kt",
            """
            package com.example.single

            import io.github.rentoyokawa.stateholder.annotations.StateHolder

            @StateHolder
            class SingleStateHolder(val repository: FakeRepository)
            """.trimIndent(),
        ),
        SourceFile.kotlin(
            "SingleViewModel.kt",
            """
            package com.example.single

            import androidx.lifecycle.ViewModel

            class SingleViewModel(val holder: SingleStateHolder) : ViewModel()
            """.trimIndent(),
        ),
    )

    private fun compile(
        sources: List<SourceFile>,
        options: Map<String, String> = emptyMap(),
    ): Pair<KotlinCompilation.Result, KotlinCompilation> {
        val compilation = KotlinCompilation().apply {
            this.sources = sources
            symbolProcessorProviders = listOf(StateHolderProcessorProvider())
            kspArgs.putAll(options)
            inheritClassPath = true
            verbose = false
        }
        return compilation.compile() to compilation
    }

    private fun generatedPackageOf(compilation: KotlinCompilation): String {
        val generatedFile = requireNotNull(
            compilation.kspSourcesDir.walkTopDown()
                .firstOrNull { it.isFile && it.name == "StateHolderModule_Generated.kt" },
        ) { "StateHolderModule_Generated.kt が生成されていません（kspSourcesDir: ${compilation.kspSourcesDir}）" }

        val packageLine = generatedFile.readLines()
            .firstOrNull { it.trim().startsWith("package ") }
            ?: error("生成ファイルにpackage宣言がありません: ${generatedFile.path}")

        return packageLine.trim().removePrefix("package").trim()
    }

    @Test
    fun `複数パッケージに分散したVMの出力先は辞書順ソート先頭パッケージになる`() {
        val (result, compilation) = compile(multiPackageSources())

        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode, result.messages)
        assertEquals("com.example.alpha.generated", generatedPackageOf(compilation))
    }

    @Test
    fun `複数パッケージに分散した入力を複数回コンパイルしても出力先パッケージは一致する`() {
        val runs = (1..3).map {
            val (result, compilation) = compile(multiPackageSources())
            assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode, result.messages)
            generatedPackageOf(compilation)
        }

        assertTrue(
            runs.all { it == runs.first() },
            "複数回のコンパイルで出力先パッケージが一致しない（非決定的）: $runs",
        )
        assertEquals("com.example.alpha.generated", runs.first())
    }

    @Test
    fun `stateholder module packageオプションを指定すると出力先パッケージがその値になる`() {
        val (result, compilation) = compile(
            multiPackageSources(),
            options = mapOf("stateholder.module.package" to "com.example.custom"),
        )

        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode, result.messages)
        assertEquals("com.example.custom.generated", generatedPackageOf(compilation))
    }

    @Test
    fun `単一パッケージのみの入力では出力先パッケージが従来どおりそのパッケージになる`() {
        val (result, compilation) = compile(singlePackageSources())

        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode, result.messages)
        assertEquals("com.example.single.generated", generatedPackageOf(compilation))
    }
}
