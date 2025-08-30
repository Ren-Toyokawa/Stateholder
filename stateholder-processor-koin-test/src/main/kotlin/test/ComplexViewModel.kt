package test

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.rentoyokawa.stateholder.annotations.SharedState
import io.github.rentoyokawa.stateholder.extensions.stateHolder
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import org.koin.core.component.KoinComponent

/**
 * 複数のStateHolderを使用するComplexViewModel
 * 
 * このViewModelは以下の機能を提供します：
 * - ユーザー管理（UserStateHolder）
 * - 商品管理（ProductStateHolder）
 * - 全体の状態管理（TestStateHolder）
 * 
 * すべてのStateHolderで汎用的な`by stateHolder()`を使用します。
 */
class ComplexViewModel : ViewModel(), KoinComponent {
    
    // SharedStateの定義
    @SharedState
    internal val userState = UserSharedState()
    
    @SharedState
    internal val productState = ProductSharedState()
    
    // 複数のStateHolderを型推論のみで注入
    val userStateHolder: UserStateHolder by stateHolder()
    
    val productStateHolder: ProductStateHolder by stateHolder()
    
    val overallStateHolder: TestStateHolder by stateHolder()
    
    // ViewModelレベルでの状態統合
    val isAnyLoading: StateFlow<Boolean> = combine(
        userStateHolder.state,
        productStateHolder.state,
        overallStateHolder.state
    ) { userState, productState, overallState ->
        userState.isLoading || productState.isLoading || overallState.isEmpty
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), true)
    
    val canProceedToCheckout: StateFlow<Boolean> = combine(
        userStateHolder.state,
        productStateHolder.state
    ) { userState, productState ->
        userState.isLoggedIn && productState.canAddToCart
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), false)
    
    // 統合されたアクション
    fun performLogin(userId: String, username: String) {
        userStateHolder.action.login(userId, username)
        overallStateHolder.action.updateUser(userId, username)
    }
    
    fun selectProduct(productId: String, productName: String, price: Int) {
        productStateHolder.action.selectProduct(productId, productName, price)
        overallStateHolder.action.updateProduct(productId, productName, price)
    }
    
    fun proceedToCheckout() {
        if (canProceedToCheckout.value) {
            productStateHolder.action.addToCart()
        }
    }
    
    fun reset() {
        userStateHolder.action.logout()
        productStateHolder.action.clearSelection()
        overallStateHolder.action.reset()
    }
}