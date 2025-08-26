package io.github.rentoyokawa.stateholder

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine

/**
 * テスト用のStateHolder実装例
 */

// Counter例用のデータクラス
data class CounterSource(
    val count: Int,
    val isLoading: Boolean,
    val error: String?
)

data class CounterState(
    val displayCount: String,
    val isLoading: Boolean,
    val hasError: Boolean
)

interface CounterAction {
    fun increment()
    fun decrement()
    fun reset()
    fun setError(message: String?)
}

/**
 * StateHolderパターンの実装例
 * カウンター状態を管理するためのStateHolderの使用方法を示す
 */
class CounterStateHolderExample(
    scope: CoroutineScope
) : StateHolder<CounterSource, CounterState, CounterAction>(scope) {
    
    private val _count: MutableStateFlow<Int> = MutableStateFlow(0)
    private val _isLoading: MutableStateFlow<Boolean> = MutableStateFlow(false)
    private val _error: MutableStateFlow<String?> = MutableStateFlow(null)
    
    override fun defineState(source: CounterSource): CounterState {
        return CounterState(
            displayCount = "Count: ${source.count}",
            isLoading = source.isLoading,
            hasError = source.error != null
        )
    }
    
    override fun createStateFlow(): Flow<CounterSource> {
        return combine(
            _count,
            _isLoading,
            _error
        ) { count, isLoading, error ->
            CounterSource(count, isLoading, error)
        }
    }
    
    override fun createInitialState(): CounterState {
        return CounterState(
            displayCount = "Count: 0",
            isLoading = false,
            hasError = false
        )
    }
    
    override val action = object : CounterAction {
        override fun increment() {
            _count.value++
        }
        
        override fun decrement() {
            _count.value--
        }
        
        override fun reset() {
            _count.value = 0
            _error.value = null
        }
        
        override fun setError(message: String?) {
            _error.value = message
        }
    }
}