package io.github.rentoyokawa.stateholder

import app.cash.turbine.test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * CounterStateHolderExampleを使用したStateHolderパターンの統合テスト
 * StateHolderの正しい使用方法を実証し検証
 */
@OptIn(ExperimentalCoroutinesApi::class)
class StateHolderExampleTest {
    
    @Test
    fun `defineStateは純粋関数であるべき`() = runTest {
        val holder = CounterStateHolderExample(backgroundScope)
        
        // 同じsourceは常に同じstateを生成するべき
        val source1 = CounterSource(5, false, null)
        val state1 = holder.defineState(source1)
        val state2 = holder.defineState(source1)
        
        assertEquals(state1, state2)
        assertEquals("Count: 5", state1.displayCount)
        assertEquals(false, state1.isLoading)
        assertEquals(false, state1.hasError)
        
        // 異なるsourceは異なるstateを生成するべき
        val source2 = CounterSource(10, true, "Error")
        val state3 = holder.defineState(source2)
        
        assertEquals("Count: 10", state3.displayCount)
        assertEquals(true, state3.isLoading)
        assertEquals(true, state3.hasError)
    }
    
    @Test
    fun `stateは初期値を即座に発行する`() = runTest {
        val holder = CounterStateHolderExample(backgroundScope)
        
        holder.state.test {
            // 初期値が即座に利用可能であることを確認
            val initialState = awaitItem()
            assertEquals("Count: 0", initialState.displayCount)
            assertEquals(false, initialState.isLoading)
            assertEquals(false, initialState.hasError)
            cancelAndIgnoreRemainingEvents()
        }
    }
    
    @Test
    fun `アクションが呼ばれたときstateが更新される`() = runTest {
        val holder = CounterStateHolderExample(backgroundScope)
        
        holder.state.test {
            // 初期状態
            assertEquals("Count: 0", awaitItem().displayCount)
            
            // インクリメント
            holder.action.increment()
            assertEquals("Count: 1", awaitItem().displayCount)
            
            // 再度インクリメント
            holder.action.increment()
            assertEquals("Count: 2", awaitItem().displayCount)
            
            // デクリメント
            holder.action.decrement()
            assertEquals("Count: 1", awaitItem().displayCount)
            
            // リセット
            holder.action.reset()
            assertEquals("Count: 0", awaitItem().displayCount)
            
            cancelAndIgnoreRemainingEvents()
        }
    }
    
    @Test
    fun `エラー状態が正しく処理される`() = runTest {
        val holder = CounterStateHolderExample(backgroundScope)
        
        holder.state.test {
            // 初期状態にエラーはない
            assertEquals(false, awaitItem().hasError)
            
            // エラーを設定
            holder.action.setError("Something went wrong")
            assertEquals(true, awaitItem().hasError)
            
            // エラーをクリア
            holder.action.setError(null)
            assertEquals(false, awaitItem().hasError)
            
            // リセットでもエラーがクリアされる
            holder.action.setError("Another error")
            assertEquals(true, awaitItem().hasError)
            
            holder.action.reset()
            assertEquals(false, awaitItem().hasError)
            
            cancelAndIgnoreRemainingEvents()
        }
    }
    
    @Test
    fun `複数のStateHolderは独立している`() = runTest {
        val holder1 = CounterStateHolderExample(backgroundScope)
        val holder2 = CounterStateHolderExample(backgroundScope)
        
        // holder1を独立してテスト
        holder1.state.test {
            // 初期状態
            assertEquals("Count: 0", awaitItem().displayCount)
            
            // holder1をインクリメント
            holder1.action.increment()
            assertEquals("Count: 1", awaitItem().displayCount)
            
            cancelAndIgnoreRemainingEvents()
        }
        
        // holder2を独立してテスト
        holder2.state.test {
            // 初期状態はまだ 0 であるべき
            assertEquals("Count: 0", awaitItem().displayCount)
            
            // holder2を2回インクリメント
            holder2.action.increment()
            assertEquals("Count: 1", awaitItem().displayCount)
            holder2.action.increment()
            assertEquals("Count: 2", awaitItem().displayCount)
            
            cancelAndIgnoreRemainingEvents()
        }
        
        // 最終状態が独立していることを検証
        assertEquals("Count: 1", holder1.state.value.displayCount)
        assertEquals("Count: 2", holder2.state.value.displayCount)
    }
    
    @Test
    fun `createInitialStateは正しい初期値を提供する`() = runTest {
        val holder = CounterStateHolderExample(backgroundScope)
        val initialState = holder.createInitialState()
        
        assertEquals("Count: 0", initialState.displayCount)
        assertEquals(false, initialState.isLoading)
        assertEquals(false, initialState.hasError)
    }
    
    @Test
    fun `actionはアクセス可能でnullではない`() = runTest {
        val holder = CounterStateHolderExample(backgroundScope)
        
        assertNotNull(holder.action)
        
        holder.state.test {
            // 初期状態
            awaitItem()
            
            // すべてのアクションメソッドが呼び出し可能であるべき
            holder.action.increment()
            awaitItem()
            holder.action.increment()  // count = 2 にする
            awaitItem()
            holder.action.decrement()
            awaitItem()
            holder.action.reset()  // count = 1 から 0 にリセット
            awaitItem()
            holder.action.setError("test")
            awaitItem()
            holder.action.setError(null)
            awaitItem()
            
            // 例外がスローされないべき
            cancelAndIgnoreRemainingEvents()
        }
    }
}