package test

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.rentoyokawa.stateholder.annotations.SharedState
import io.github.rentoyokawa.stateholder.extensions.stateHolder
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.koin.core.component.KoinComponent

/**
 * KSPプロセッサーのテスト用クラス（新実装）
 * 
 * 設計書準拠の実装：
 * - SharedStateをViewModel内で定義
 * - StateHolderはstateHolder()拡張関数で注入
 * - ViewModelレベルでの状態統合
 */
class TestViewModel : ViewModel(), KoinComponent {
    
    @SharedState
    internal val userState = UserSharedState()
    
    @SharedState
    internal val productState = ProductSharedState()
    
    // 新実装では@InjectStateHolderは使用せず、stateHolder()のみを使用
    val testStateHolderGeneric: TestStateHolder by stateHolder()
    
    // ViewModelレベルでの状態統合
    val isEmpty: StateFlow<Boolean> = testStateHolderGeneric.state
        .map { it.isEmpty }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), true)
    
    fun loadTestData() {
        userState.userId = "test-user-123"
        productState.productId = "test-product-456"
    }
    
    fun observeState() {
        // StateHolderのstateを監視する例
        // val state = testStateHolderGeneric.state.value
    }
    
    fun performAction() {
        // StateHolderのactionを実行する例
        testStateHolderGeneric.action.updateUser("new-user", "New User Name")
        testStateHolderGeneric.action.updateProduct("new-product", "New Product", 1000)
    }
}