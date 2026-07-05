@file:OptIn(org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi::class)

package io.github.rentoyokawa.stateholder.ksp

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import com.tschuchort.compiletesting.kspArgs
import com.tschuchort.compiletesting.kspSourcesDir
import com.tschuchort.compiletesting.symbolProcessorProviders
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 生成Koinモジュールの出力先パッケージ決定ロジックの決定性を検証するテスト。
 *
 * `StateHolderProcessor.generateKoinModule()` は以前、`@StateHolder` クラスの
 * 列挙順（`getSymbolsWithAnnotation()` の非決定的な順序）に依存する `first()` を
 * 使って出力先パッケージを決めていた（BuildApp CI でのフレークの根本原因）。
 * このテストは、修正後のロジック（KSPオプション優先 → distinctパッケージの辞書順ソート）
 * が列挙順に依存せず常に同じ結果を返すことを保証する。
 */
class DeterministicPackageTest {

    /** 2つのパッケージに分散した @StateHolder クラスを含むソース一式。
     *  辞書順で "com.example.alpha" が "com.example.zeta" より先になるようにしている。 */
    private fun multiPackageSources(): List<SourceFile> = listOf(
        SourceFile.kotlin(
            "ZetaStateHolder.kt",
            """
            package com.example.zeta

            import io.github.rentoyokawa.stateholder.annotations.StateHolder

            @StateHolder
            class ZetaStateHolder
            """.trimIndent()
        ),
        SourceFile.kotlin(
            "AlphaStateHolder.kt",
            """
            package com.example.alpha

            import io.github.rentoyokawa.stateholder.annotations.StateHolder

            @StateHolder
            class AlphaStateHolder
            """.trimIndent()
        )
    )

    private fun singlePackageSources(): List<SourceFile> = listOf(
        SourceFile.kotlin(
            "SingleStateHolder.kt",
            """
            package com.example.single

            import io.github.rentoyokawa.stateholder.annotations.StateHolder

            @StateHolder
            class SingleStateHolder
            """.trimIndent()
        )
    )

    private fun compile(
        sources: List<SourceFile>,
        options: Map<String, String> = emptyMap()
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

    /** kspSourcesDir配下から生成された StateHolderModule_Generated.kt の package 宣言を取得する。 */
    private fun generatedPackageOf(compilation: KotlinCompilation): String {
        val generatedFile = requireNotNull(
            compilation.kspSourcesDir.walkTopDown()
                .firstOrNull { it.isFile && it.name == "StateHolderModule_Generated.kt" }
        ) { "StateHolderModule_Generated.kt が生成されていません（kspSourcesDir: ${compilation.kspSourcesDir}）" }

        val packageLine = generatedFile.readLines()
            .firstOrNull { it.trim().startsWith("package ") }
            ?: error("生成ファイルにpackage宣言がありません: ${generatedFile.path}")

        return packageLine.trim().removePrefix("package").trim()
    }

    @Test
    fun `複数パッケージに分散したStateHolderの出力先は辞書順ソート先頭パッケージになる`() {
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
            "複数回のコンパイルで出力先パッケージが一致しない（非決定的）: $runs"
        )
        assertEquals("com.example.alpha.generated", runs.first())
    }

    @Test
    fun `stateholder module packageオプションを指定すると出力先パッケージがその値になる`() {
        val (result, compilation) = compile(
            multiPackageSources(),
            options = mapOf("stateholder.module.package" to "com.example.custom")
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
