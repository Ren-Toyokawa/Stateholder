package test

import io.github.rentoyokawa.stateholder.StateHolder
import io.github.rentoyokawa.stateholder.annotations.InjectedParam
import io.github.rentoyokawa.stateholder.annotations.StateHolder as StateHolderAnnotation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

// Source data (internal state)
data class ProductSource(
    val productState: ProductSharedState,
    val isLoading: Boolean,
    val selectedQuantity: Int
)

// UI state (external state)
data class ProductState(
    val productName: String,
    val productId: String,
    val price: String,
    val quantity: Int,
    val totalPrice: String,
    val isLoading: Boolean,
    val canAddToCart: Boolean
)

// Actions interface
interface ProductAction {
    fun selectProduct(productId: String, productName: String, price: Int)
    fun updateQuantity(quantity: Int)
    fun addToCart()
    fun clearSelection()
}

@StateHolderAnnotation
class ProductStateHolder(
    @InjectedParam("productState") private val productState: ProductSharedState,
    scope: CoroutineScope
) : StateHolder<ProductSource, ProductState, ProductAction>(scope) {
    
    private val _isLoading = MutableStateFlow(false)
    private val _selectedQuantity = MutableStateFlow(1)
    
    override fun defineState(source: ProductSource): ProductState {
        val totalPrice = source.productState.price * source.selectedQuantity
        
        return ProductState(
            productName = source.productState.productName,
            productId = source.productState.productId,
            price = "¥${source.productState.price}",
            quantity = source.selectedQuantity,
            totalPrice = "¥${totalPrice}",
            isLoading = source.isLoading,
            canAddToCart = source.productState.productId.isNotEmpty() && source.selectedQuantity > 0
        )
    }
    
    override fun createStateFlow(): Flow<ProductSource> {
        return kotlinx.coroutines.flow.combine(
            _isLoading,
            _selectedQuantity
        ) { isLoading, quantity ->
            ProductSource(productState, isLoading, quantity)
        }
    }
    
    override fun createInitialState(): ProductState {
        return ProductState(
            productName = "",
            productId = "",
            price = "¥0",
            quantity = 1,
            totalPrice = "¥0",
            isLoading = false,
            canAddToCart = false
        )
    }
    
    override val action = object : ProductAction {
        override fun selectProduct(productId: String, productName: String, price: Int) {
            _isLoading.value = true
            productState.productId = productId
            productState.productName = productName
            productState.price = price
            _isLoading.value = false
        }
        
        override fun updateQuantity(quantity: Int) {
            if (quantity > 0) {
                _selectedQuantity.value = quantity
            }
        }
        
        override fun addToCart() {
            _isLoading.value = true
            // カートに追加する処理をシミュレート
            _isLoading.value = false
        }
        
        override fun clearSelection() {
            productState.productId = ""
            productState.productName = ""
            productState.price = 0
            _selectedQuantity.value = 1
        }
    }
}