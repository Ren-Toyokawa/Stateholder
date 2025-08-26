package io.github.rentoyokawa.stateholder

import app.cash.turbine.test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * StateHolder抽象クラスのユニットテスト
 * StateHolderパターンの核心機能を検証
 */
@OptIn(ExperimentalCoroutinesApi::class)
class StateHolderTest {
    
    @Test
    fun `stateはcreateInitialStateの値で初期化される`() = runTest {
        val holder = MinimalStateHolder(backgroundScope)
        
        // createInitialState()の値で初期化されることを検証
        assertEquals("initial", holder.state.value)
    }
    
    @Test
    fun `stateはdefineStateを通じてsourceを変換する`() = runTest {
        val holder = TransformingStateHolder(backgroundScope)
        
        holder.state.test {
            // 初期状態
            assertEquals("Value: 0", awaitItem())
            
            // sourceを更新
            holder.updateSource(5)
            assertEquals("Value: 5", awaitItem())
            
            holder.updateSource(10)
            assertEquals("Value: 10", awaitItem())
            
            cancelAndIgnoreRemainingEvents()
        }
    }
    
    @Test
    fun `stateは初期化順序問題を回避するためにlazy初期化を使用する`() = runTest {
        // lazyがない場合に失敗するStateHolderを作成して
        // lazy初期化が正しく動作することを検証
        val holder = DelayedInitStateHolder(backgroundScope)
        
        holder.state.test {
            // 初期状態はcreateInitialState()から取得
            assertEquals("Initialized: false", awaitItem())
            
            // 初期化後、flowはtrueを発行
            holder.updateInitialized(true)
            assertEquals("Initialized: true", awaitItem())
            
            cancelAndIgnoreRemainingEvents()
        }
    }
    
    @Test
    fun `stateはSharingStarted EagerlyでFlowを共有する`() = runTest {
        val holder = MinimalStateHolder(backgroundScope)
        
        // stateに複数回アクセス - 同じStateFlowインスタンスを取得するべき
        val state1 = holder.state
        val state2 = holder.state
        
        // lazy初期化により同じインスタンスであるべき
        assertEquals(state1, state2)
        
        // 値は即座に利用可能であるべき（Eagerly）
        assertEquals("initial", state1.value)
    }
}

/**
 * StateHolderの基本機能をテストするための最小実装
 */
private class MinimalStateHolder(
    scope: kotlinx.coroutines.CoroutineScope
) : StateHolder<Int, String, Unit>(scope) {
    
    override fun defineState(source: Int): String = "Value: $source"
    
    override fun createStateFlow() = flowOf(0)
    
    override fun createInitialState(): String = "initial"
    
    override val action = Unit
}

/**
 * 変換をテストするためのsource更新可能な実装
 */
private class TransformingStateHolder(
    scope: kotlinx.coroutines.CoroutineScope
) : StateHolder<Int, String, Unit>(scope) {
    
    private val sourceFlow = MutableStateFlow(0)
    
    override fun defineState(source: Int): String = "Value: $source"
    
    override fun createStateFlow() = sourceFlow
    
    override fun createInitialState(): String = "Value: 0"
    
    override val action = Unit
    
    fun updateSource(value: Int) {
        sourceFlow.value = value
    }
}

/**
 * 遅延初期化をテストする実装
 * lazy初期化がない場合は失敗する
 */
private class DelayedInitStateHolder(
    scope: kotlinx.coroutines.CoroutineScope
) : StateHolder<Boolean, String, Unit>(scope) {
    
    // このプロパティは親コンストラクタの後に初期化される
    private val isInitialized = MutableStateFlow(false)
    
    override fun defineState(source: Boolean): String = "Initialized: $source"
    
    override fun createStateFlow() = isInitialized
    
    override fun createInitialState(): String = "Initialized: false"
    
    override val action = Unit
    
    fun updateInitialized(value: Boolean) {
        isInitialized.value = value
    }
}