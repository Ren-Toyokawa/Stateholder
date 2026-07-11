package io.github.rentoyokawa.stateholder.core

import app.cash.turbine.test
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Store の核心セマンティクスの検証:
 * - Local の保持と update による不変更新
 * - Local と外部データの combine 合成（sources）
 * - defineState（純関数）による State 導出
 * - 購読が絶えても Local が保持されること
 */
class StoreTest {

    private data class CounterLocal(val count: Int = 0)

    private data class CounterSource(val count: Int, val external: Int)

    /** テスト用の専用 Store（Local + 外部入力を combine 合成する） */
    private class CounterStore(
        scope: CoroutineScope,
        private val external: Flow<Int> = flowOf(0),
    ) : Store<CounterLocal, CounterSource, String>(scope) {
        override val initialLocal = CounterLocal()
        override val initialState = "count=0,ext=0"
        override fun sources(local: Flow<CounterLocal>): Flow<CounterSource> =
            combine(local, external) { l, e -> CounterSource(l.count, e) }
        override fun defineState(source: CounterSource) = "count=${source.count},ext=${source.external}"
    }

    @Test
    fun `初期stateはinitialStateの値になる`() = runTest {
        val store = CounterStore(backgroundScope)
        assertEquals("count=0,ext=0", store.state.value)
    }

    @Test
    fun `LocalのupdateがsourcesとdefineState経由でstateに反映される`() = runTest {
        val store = CounterStore(backgroundScope)
        store.state.test {
            assertEquals("count=0,ext=0", awaitItem())

            store.update { it.copy(count = it.count + 1) }
            assertEquals("count=1,ext=0", awaitItem())

            store.update { it.copy(count = 10) }
            assertEquals("count=10,ext=0", awaitItem())
        }
    }

    @Test
    fun `copyによる不変更新は毎回emitされる`() = runTest {
        // 旧設計の「同一インスタンス再代入で emit されない」バグが
        // update + copy の一本化で構造的に起きないことの確認
        val store = CounterStore(backgroundScope)
        store.state.test {
            assertEquals("count=0,ext=0", awaitItem())
            store.update { it.copy(count = 1) }
            assertEquals("count=1,ext=0", awaitItem())
            store.update { it.copy(count = 2) }
            assertEquals("count=2,ext=0", awaitItem())
        }
    }

    @Test
    fun `外部データがcombine合成でstateに反映される`() = runTest {
        val external = MutableStateFlow(0)
        val store = CounterStore(scope = backgroundScope, external = external)

        store.state.test {
            assertEquals("count=0,ext=0", awaitItem())

            external.value = 42
            assertEquals("count=0,ext=42", awaitItem())

            external.value = 7
            assertEquals("count=0,ext=7", awaitItem())
        }
    }

    @Test
    fun `ユーザー操作(Local update)と外部データが同じSourceに合成される`() = runTest {
        val external = MutableStateFlow(100)
        val store = CounterStore(scope = backgroundScope, external = external)

        store.state.test {
            skipItems(1) // 初期値

            external.value = 5
            assertEquals("count=0,ext=5", awaitItem())

            store.update { it.copy(count = it.count + 1) }
            assertEquals("count=1,ext=5", awaitItem())
        }
    }

    @Test
    fun `購読が絶えてもLocalは保持され再購読で最新値から始まる`() = runTest {
        val store = CounterStore(backgroundScope)

        store.state.test {
            assertEquals("count=0,ext=0", awaitItem())
            store.update { it.copy(count = 3) }
            assertEquals("count=3,ext=0", awaitItem())
        }

        // 購読終了後も Local は Store が保持している
        assertEquals(3, store.currentLocal.count)

        // 再購読しても書き込んだ値は失われていない
        store.state.test {
            assertEquals("count=3,ext=0", awaitItem())
        }
    }

    @Test
    fun `defineStateは純関数として直接テストできる`() {
        // Store を経由せず、変換ロジック単体を検証できることの確認
        val defineState: (CounterSource) -> String = { "count=${it.count},ext=${it.external}" }
        assertEquals("count=99,ext=1", defineState(CounterSource(count = 99, external = 1)))
    }
}
