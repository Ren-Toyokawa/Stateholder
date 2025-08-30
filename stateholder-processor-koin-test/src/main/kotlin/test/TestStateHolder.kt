package test

import io.github.rentoyokawa.stateholder.StateHolder
import io.github.rentoyokawa.stateholder.annotations.InjectedParam
import io.github.rentoyokawa.stateholder.annotations.StateHolder as StateHolderAnnotation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine

// Source data (internal state)
data class TestSource(
    val userState: UserSharedState,
    val productState: ProductSharedState
)

// UI state (external state)
data class TestState(
    val userInfo: String,
    val productInfo: String,
    val isEmpty: Boolean
)

// Actions interface
interface TestAction {
    fun updateUser(userId: String, userName: String)
    fun updateProduct(productId: String, productName: String, price: Int)
    fun reset()
}

@StateHolderAnnotation
class TestStateHolder(
    @InjectedParam("userState") private val userState: UserSharedState,
    @InjectedParam("productState") private val productState: ProductSharedState,
    scope: CoroutineScope
) : StateHolder<TestSource, TestState, TestAction>(scope) {
    
    private val _userStateFlow = MutableStateFlow(userState)
    private val _productStateFlow = MutableStateFlow(productState)
    
    override fun defineState(source: TestSource): TestState {
        return TestState(
            userInfo = "User: ${source.userState.userId} - ${source.userState.userName}",
            productInfo = "Product: ${source.productState.productId} - ${source.productState.productName} (¥${source.productState.price})",
            isEmpty = source.userState.userId.isEmpty() && source.productState.productId.isEmpty()
        )
    }
    
    override fun createStateFlow(): Flow<TestSource> {
        return combine(
            _userStateFlow,
            _productStateFlow
        ) { user, product ->
            TestSource(user, product)
        }
    }
    
    override fun createInitialState(): TestState {
        return TestState(
            userInfo = "User: - ",
            productInfo = "Product: - (¥0)",
            isEmpty = true
        )
    }
    
    override val action = object : TestAction {
        override fun updateUser(userId: String, userName: String) {
            userState.userId = userId
            userState.userName = userName
            _userStateFlow.value = userState
        }
        
        override fun updateProduct(productId: String, productName: String, price: Int) {
            productState.productId = productId
            productState.productName = productName
            productState.price = price
            _productStateFlow.value = productState
        }
        
        override fun reset() {
            userState.userId = ""
            userState.userName = ""
            productState.productId = ""
            productState.productName = ""
            productState.price = 0
            _userStateFlow.value = userState
            _productStateFlow.value = productState
        }
    }
}

data class UserSharedState(
    var userId: String = "",
    var userName: String = ""
)

data class ProductSharedState(
    var productId: String = "",
    var productName: String = "",
    var price: Int = 0
)