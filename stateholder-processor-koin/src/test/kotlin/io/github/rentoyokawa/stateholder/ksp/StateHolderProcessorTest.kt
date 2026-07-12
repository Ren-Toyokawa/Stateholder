@file:OptIn(org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi::class)

package io.github.rentoyokawa.stateholder.ksp

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import com.tschuchort.compiletesting.kspArgs
import com.tschuchort.compiletesting.kspSourcesDir
import com.tschuchort.compiletesting.kspWithCompilation
import com.tschuchort.compiletesting.symbolProcessorProviders
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 新設計（[[STA-13]]）向けに刷新した [StateHolderProcessor] の e2e テスト。
 *
 * `@StateHolder` 具象 holder をコンストラクタで受け取る ViewModel を検出し、
 * Koin `factory { }` 定義（`viewModel {}` を選ばない理由は [[STA-14]] intent 参照）を生成することを、
 * 実際に KSP を適用したコンパイル結果で検証する。
 */
class StateHolderProcessorTest {

    private val repositorySource = SourceFile.kotlin(
        "FakeUserRepository.kt",
        """
        package com.example.repo

        class FakeUserRepository
        """.trimIndent(),
    )

    /** 一覧 holder + 詳細 holder が同じ SharedState<String?> を共有するシナリオ（受入基準の核） */
    private fun sharedScenarioSources(): List<SourceFile> = listOf(
        repositorySource,
        SourceFile.kotlin(
            "UserListStateHolder.kt",
            """
            package com.example.list

            import io.github.rentoyokawa.stateholder.annotations.StateHolder
            import io.github.rentoyokawa.stateholder.core.SharedState
            import com.example.repo.FakeUserRepository
            import kotlinx.coroutines.CoroutineScope

            @StateHolder
            class UserListStateHolder(
                val selectedUserId: SharedState<String?>,
                val repository: FakeUserRepository,
                val scope: CoroutineScope,
            )
            """.trimIndent(),
        ),
        SourceFile.kotlin(
            "UserDetailStateHolder.kt",
            """
            package com.example.detail

            import io.github.rentoyokawa.stateholder.annotations.StateHolder
            import io.github.rentoyokawa.stateholder.core.SharedState
            import com.example.repo.FakeUserRepository
            import kotlinx.coroutines.CoroutineScope

            @StateHolder
            class UserDetailStateHolder(
                val selectedUserId: SharedState<String?>,
                val repository: FakeUserRepository,
                val scope: CoroutineScope,
            )
            """.trimIndent(),
        ),
        SourceFile.kotlin(
            "UserViewModel.kt",
            """
            package com.example.vm

            import androidx.lifecycle.ViewModel
            import com.example.list.UserListStateHolder
            import com.example.detail.UserDetailStateHolder

            class UserViewModel(
                val list: UserListStateHolder,
                val detail: UserDetailStateHolder,
            ) : ViewModel()
            """.trimIndent(),
        ),
    )

    private fun compile(
        sources: List<SourceFile>,
        options: Map<String, String> = emptyMap(),
        withCompilation: Boolean = true,
    ): Pair<KotlinCompilation.Result, KotlinCompilation> {
        val compilation = KotlinCompilation().apply {
            this.sources = sources
            symbolProcessorProviders = listOf(StateHolderProcessorProvider())
            kspArgs.putAll(options)
            kspWithCompilation = withCompilation
            inheritClassPath = true
            verbose = false
        }
        return compilation.compile() to compilation
    }

    private fun generatedFileContent(compilation: KotlinCompilation): String {
        val generatedFile = requireNotNull(
            compilation.kspSourcesDir.walkTopDown()
                .firstOrNull { it.isFile && it.name == "StateHolderModule_Generated.kt" },
        ) { "StateHolderModule_Generated.kt が生成されていません（kspSourcesDir: ${compilation.kspSourcesDir}）" }
        return generatedFile.readText()
    }

    /**
     * KotlinPoet は import 済みクラスを短縮名で出力し、長い行を折り返す（複数行にまたがる）ため、
     * 構造的な文字列検証は改行・連続空白を単一スペースへ正規化してから行う。
     */
    private fun normalized(text: String): String = text.replace(Regex("\\s+"), " ")

    @Test
    fun `同型SharedStateを要求する2holderを持つVMはfactory定義を1つ生成しコンパイル成功する`() {
        val (result, compilation) = compile(sharedScenarioSources())

        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode, result.messages)

        val generated = normalized(generatedFileContent(compilation))
        assertTrue(generated.contains("factory<UserViewModel>"), generated)
        // 1 VM = 1 factory 定義のみ
        assertEquals(1, Regex("factory<").findAll(generated).count(), generated)
    }

    @Test
    fun `同一VM内で同型SharedStateを要求する2holderは同一インスタンス(同一ローカル変数)を共有する`() {
        val (result, compilation) = compile(sharedScenarioSources())
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode, result.messages)

        val generated = normalized(generatedFileContent(compilation))

        // SharedState<String?> のインスタンス生成は VM につき1本だけ
        assertEquals(
            1,
            Regex("""SharedState<String\?>\(null\)""").findAll(generated).count(),
            generated,
        )
        // 一覧・詳細の両 holder コンストラクタが同じローカル変数を named 引数で受け取る（同一インスタンス結線）
        assertEquals(
            2,
            Regex("""selectedUserId = selectedUserId""").findAll(generated).count(),
            generated,
        )
    }

    @Test
    fun `CoroutineScope引数を持つholderにはVMごとに共有scopeが渡り他の引数はget()で解決される`() {
        val (result, compilation) = compile(sharedScenarioSources())
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode, result.messages)

        val generated = normalized(generatedFileContent(compilation))
        assertTrue(generated.contains("val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)"), generated)
        // scope は VM につき1回生成のみ（2 holder が共有）
        assertEquals(1, Regex("""val scope = CoroutineScope""").findAll(generated).count(), generated)
        assertEquals(2, Regex("""scope = scope""").findAll(generated).count(), generated)
        assertEquals(2, Regex("""repository = get\(\)""").findAll(generated).count(), generated)
    }

    @Test
    fun `holderは個別Koin定義として登録されずVM構築ブロック内で具象型インライン構築される`() {
        val (result, compilation) = compile(sharedScenarioSources())
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode, result.messages)

        val generated = normalized(generatedFileContent(compilation))
        // 個別の factory<UserListStateHolder> / factory<UserDetailStateHolder> は存在しない
        assertTrue(!generated.contains("factory<UserListStateHolder>"), generated)
        assertTrue(!generated.contains("factory<UserDetailStateHolder>"), generated)
        // VM 構築式の中に具象 holder コンストラクタ呼び出しが現れる（短縮名で import 済み）
        assertTrue(generated.contains("UserListStateHolder(selectedUserId = selectedUserId"), generated)
        assertTrue(generated.contains("UserDetailStateHolder(selectedUserId = selectedUserId"), generated)
    }

    @Test
    fun `非nullableなSharedState型はコンパイルエラーになる`() {
        val sources = listOf(
            repositorySource,
            SourceFile.kotlin(
                "InvalidStateHolder.kt",
                """
                package com.example.invalid

                import io.github.rentoyokawa.stateholder.annotations.StateHolder
                import io.github.rentoyokawa.stateholder.core.SharedState
                import com.example.repo.FakeUserRepository

                @StateHolder
                class InvalidStateHolder(
                    val count: SharedState<Int>,
                    val repository: FakeUserRepository,
                )
                """.trimIndent(),
            ),
        )

        val (result, _) = compile(sources, withCompilation = false)

        assertEquals(KotlinCompilation.ExitCode.COMPILATION_ERROR, result.exitCode)
        assertTrue(result.messages.contains("nullable"), result.messages)
    }

    @Test
    fun `対応するVMが無いholderのみの入力では生成をスキップする`() {
        val sources = listOf(
            repositorySource,
            SourceFile.kotlin(
                "OrphanStateHolder.kt",
                """
                package com.example.orphan

                import io.github.rentoyokawa.stateholder.annotations.StateHolder
                import com.example.repo.FakeUserRepository

                @StateHolder
                class OrphanStateHolder(
                    val repository: FakeUserRepository,
                )
                """.trimIndent(),
            ),
        )

        val (result, compilation) = compile(sources, withCompilation = false)
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode, result.messages)

        val generatedFile = compilation.kspSourcesDir.walkTopDown()
            .firstOrNull { it.isFile && it.name == "StateHolderModule_Generated.kt" }
        assertEquals(null, generatedFile, "対象VMが無い場合は何も生成されないはず")
    }
}
