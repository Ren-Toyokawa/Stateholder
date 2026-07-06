package io.github.rentoyokawa.stateholder.core

import app.cash.turbine.test
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Store の核心セマンティクスの検証:
 * - Source の保持と update による不変更新
 * - defineState（純関数）による State 導出
 * - inputs（外部入力）の合流
 * - 購読が絶えても Source が保持されること
 */
class StoreTest {

    private data class CounterSource(val count: Int = 0, val step: Int = 1)

    private fun newStore(
        scope: kotlinx.coroutines.CoroutineScope,
        inputs: List<kotlinx.coroutines.flow.Flow<(CounterSource) -> CounterSource>> = emptyList(),
    ) = Store(
        initial = CounterSource(),
        scope = scope,
        inputs = inputs,
    ) { source -> "count=${source.count}" }

    @Test
    fun `初期stateはdefineState(initial)の値になる`() = runTest {
        val store = newStore(backgroundScope)
        assertEquals("count=0", store.state.value)
    }

    @Test
    fun `updateがdefineState経由でstateに反映される`() = runTest {
        val store = newStore(backgroundScope)
        store.state.test {
            assertEquals("count=0", awaitItem())

            store.update { it.copy(count = it.count + it.step) }
            assertEquals("count=1", awaitItem())

            store.update { it.copy(count = 10) }
            assertEquals("count=10", awaitItem())
        }
    }

    @Test
    fun `copyによる不変更新は毎回emitされる`() = runTest {
        // 旧設計の「同一インスタンス再代入で emit されない」バグが
        // update + copy の一本化で構造的に起きないことの確認
        val store = newStore(backgroundScope)
        store.state.test {
            assertEquals("count=0", awaitItem())
            store.update { it.copy(count = 1) }
            assertEquals("count=1", awaitItem())
            store.update { it.copy(count = 2) }
            assertEquals("count=2", awaitItem())
        }
    }

    @Test
    fun `外部入力(inputs)が購読中にSourceへ合流する`() = runTest {
        val external = MutableStateFlow(0)
        val store = newStore(
            scope = backgroundScope,
            inputs = listOf(
                input(external) { source, value -> source.copy(count = value) },
            ),
        )

        store.state.test {
            assertEquals("count=0", awaitItem())

            external.value = 42
            assertEquals("count=42", awaitItem())

            external.value = 7
            assertEquals("count=7", awaitItem())
        }
    }

    @Test
    fun `ユーザー操作(update)と外部入力(inputs)が同じSourceに合流する`() = runTest {
        val external = MutableStateFlow(100)
        val store = newStore(
            scope = backgroundScope,
            inputs = listOf(
                input(external) { source, value -> source.copy(count = value) },
            ),
        )

        store.state.test {
            skipItems(1) // 初期値

            external.value = 5
            assertEquals("count=5", awaitItem())

            // ユーザー操作による更新は外部入力の値の上から適用される
            store.update { it.copy(count = it.count + 1) }
            assertEquals("count=6", awaitItem())
        }
    }

    @Test
    fun `購読が絶えてもSourceは保持され再購読で最新値から始まる`() = runTest {
        val store = newStore(backgroundScope)

        store.state.test {
            assertEquals("count=0", awaitItem())
            store.update { it.copy(count = 3) }
            assertEquals("count=3", awaitItem())
        }

        // 購読終了後も Source は Store が保持している
        assertEquals(3, store.currentSource.count)

        // 再購読しても値は失われていない
        store.state.test {
            assertEquals("count=3", awaitItem())
        }
    }

    @Test
    fun `defineStateは純関数として直接テストできる`() {
        // Store を経由せず、変換ロジック単体を検証できることの確認
        val defineState: (CounterSource) -> String = { "count=${it.count}" }
        assertEquals("count=99", defineState(CounterSource(count = 99)))
    }
}
