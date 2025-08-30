# StateHolder

[![Kotlin](https://img.shields.io/badge/kotlin-2.0.10-blue.svg?logo=kotlin)](http://kotlinlang.org)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

> **Warning**: This is an experimental project. Breaking changes occur frequently. Currently designed for Compose Multiplatform and Koin, it may not work with regular Android projects.

This library implements the StateHolders pattern from [Android's official Architecture Guide](https://developer.android.com/topic/architecture#ui-layer) to reduce ViewModel responsibilities.
Built with Kotlin Multiplatform and Compose Multiplatform.

## Before / After

### Before: Complex ViewModel
```kotlin
class MainViewModel : ViewModel() {
    // Authentication related
    private val _authState = MutableStateFlow(AuthState())
    val authState: StateFlow<AuthState> = _authState
    private val authRepository = AuthRepository()
    
    fun login(email: String, password: String) { /* ... */ }
    fun logout() { /* ... */ }
    fun updateProfile(name: String) { /* ... */ }
    
    // Cart related
    private val _cartItems = MutableStateFlow<List<CartItem>>(emptyList())
    val cartItems: StateFlow<List<CartItem>> = _cartItems
    private val cartRepository = CartRepository()
    
    fun addToCart(productId: String) { /* ... */ }
    fun removeFromCart(itemId: String) { /* ... */ }
    fun calculateTotal(): Double { /* ... */ }
    
    // Product related
    private val _products = MutableStateFlow<List<Product>>(emptyList())
    val products: StateFlow<List<Product>> = _products
    private val productRepository = ProductRepository()
    
    fun loadProducts() { /* ... */ }
    fun searchProducts(query: String) { /* ... */ }
    fun filterByCategory(category: String) { /* ... */ }
    
    // Complex state combination
    val canCheckout = combine(authState, cartItems) { auth, cart ->
        auth.isAuthenticated && cart.isNotEmpty()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), false)
    
    // Many more mixed logics...
}
```

### After: Organized with StateHolders

#### ViewModel
```kotlin
class MainViewModel : ViewModel() {
    // SharedState declarations
    @SharedState internal val authState = AuthSharedState()
    @SharedState internal val cartState = CartSharedState()
    
    // StateHolder delegations
    val auth by stateHolder<AuthStateHolder>()
    val cart by stateHolder<CartStateHolder>()
    val product by stateHolder<ProductStateHolder>()
}
```

#### StateHolders
```kotlin
// Each feature has its own independent StateHolder
@StateHolder
class AuthStateHolder(/* DI params */) : StateHolder<AuthSource, AuthUiState, AuthAction>(scope), AuthAction {
    // Type definitions
    data class AuthSource(/* ... */)
    data class AuthUiState(/* ... */)
    interface AuthAction { /* ... */ }
    
    // Pure function for state transformation
    override fun defineState(source: AuthSource): AuthUiState { /* ... */ }
    
    // Data source combination
    override fun createStateFlow(): Flow<AuthSource> { /* ... */ }
    
    // Initial state
    override fun createInitialState(): AuthUiState { /* ... */ }
    
    // Action implementation
    override val action = this
}

@StateHolder
class CartStateHolder(/* ... */) : StateHolder<CartSource, CartUiState, CartAction>(scope) {
    // Same: defineState, createStateFlow, createInitialState, action
}

@StateHolder  
class ProductStateHolder(/* ... */) : StateHolder<ProductSource, ProductUiState, ProductAction>(scope) {
    // Same: defineState, createStateFlow, createInitialState, action
}
```

## StateHolder Operating Principles

StateHolders manage state through the **Source → State → Action** cycle.

### 1. Source Generation
The `createStateFlow()` method monitors multiple data sources, detects changes, and generates Source objects.

```kotlin
// Combine Repository, SharedState, and local state
override fun createStateFlow(): Flow<AuthSource> {
    return combine(
        authRepository.observeCurrentUser(),  // External data
        authSharedState.observe(),           // Shared state
        loadingState                         // Local state
    ) { user, shared, loading ->
        AuthSource(user, shared.isAuthenticated, loading)
    }
}
```

### 2. State Transformation
The `defineState()` pure function transforms Source to UI-optimized State. This function has no side effects and always returns the same State for the same Source.

```kotlin
override fun defineState(source: AuthSource): AuthUiState {
    return AuthUiState(
        displayName = source.user?.name ?: "Guest",
        isAuthenticated = source.isAuthenticated,
        canPerformAction = source.isAuthenticated && !source.isLoading
    )
}
```

### 3. Action-driven Changes
When Actions are executed from UI, StateHolder updates necessary data sources. This triggers Source regeneration and new State flows to UI.

```kotlin
override suspend fun login(email: String, password: String) {
    loadingState.value = true                // Update local state
    val result = authRepository.login(email) // Update Repository
    loadingState.value = false
    // → Automatically regenerates Source and reflects new State to UI
}
```

### Unidirectional Data Flow
This design establishes a unidirectional flow of **Action → Data Update → Source Generation → State Transformation → UI Update**, achieving predictable state changes and easy debugging.

## Technical Constraints

### Requirements

- **Koin**: Required for dependency injection
- **KSP**: Required for code generation
- **ViewModelScope**: Required for StateHolder scope management

### Design Constraints

- **No Dagger/Hilt support**: Designed for Koin-based projects
- **SharedState mutability**: Intentionally mutable for change detection
- **No direct StateHolder references**: Communication only through SharedState

## Installation

```bash
git clone https://github.com/Ren-Toyokawa/stateholder-kmp.git
cd stateholder-kmp
./gradlew publishToMavenLocal
```

```kotlin
dependencies {
    implementation("io.github.rentoyokawa:stateholder-annotations:0.1.0")
    implementation("io.github.rentoyokawa:stateholder-core:0.1.0") 
    implementation("io.github.rentoyokawa:stateholder-viewmodel-koin:0.1.0")
    ksp("io.github.rentoyokawa:stateholder-processor-koin:0.1.0")
    
    implementation("io.insert-koin:koin-core:3.5.6")
}
```

## License

Apache License 2.0

## Author

**Ren Toyokawa** - [@Ren-Toyokawa](https://github.com/Ren-Toyokawa)