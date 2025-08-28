# StateHolder

[![Kotlin](https://img.shields.io/badge/kotlin-2.0.10-blue.svg?logo=kotlin)](http://kotlinlang.org)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

> **警告**: 実験的プロジェクトです。破壊的変更が頻繁に発生します。Compose Multiplatform, Koin を前提に作成しているため、現時点では通常のAndroidプロジェクトでは使用できない可能性があります。

[Android公式アーキテクチャガイド](https://developer.android.com/topic/architecture#ui-layer)のStateHoldersパターンを用いてViewModelの責務を薄くすることを目的としています。
Kotlin Multiplatform, Compose Multiplatformで実装しています。

## Before / After

### Before: 複雑なViewModel
```kotlin
class MainViewModel : ViewModel() {
    // 認証関連
    private val _authState = MutableStateFlow(AuthState())
    val authState: StateFlow<AuthState> = _authState
    private val authRepository = AuthRepository()
    
    fun login(email: String, password: String) { /* ... */ }
    fun logout() { /* ... */ }
    fun updateProfile(name: String) { /* ... */ }
    
    // カート関連
    private val _cartItems = MutableStateFlow<List<CartItem>>(emptyList())
    val cartItems: StateFlow<List<CartItem>> = _cartItems
    private val cartRepository = CartRepository()
    
    fun addToCart(productId: String) { /* ... */ }
    fun removeFromCart(itemId: String) { /* ... */ }
    fun calculateTotal(): Double { /* ... */ }
    
    // 商品関連
    private val _products = MutableStateFlow<List<Product>>(emptyList())
    val products: StateFlow<List<Product>> = _products
    private val productRepository = ProductRepository()
    
    fun loadProducts() { /* ... */ }
    fun searchProducts(query: String) { /* ... */ }
    fun filterByCategory(category: String) { /* ... */ }
    
    // 複雑な状態結合
    val canCheckout = combine(authState, cartItems) { auth, cart ->
        auth.isAuthenticated && cart.isNotEmpty()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), false)
    
    // 他にも多くのロジックが混在...
}
```

### After: StateHolderで整理

#### ViewModel
```kotlin
class MainViewModel : ViewModel() {
    // SharedStateの宣言
    @SharedState internal val authState = AuthSharedState()
    @SharedState internal val cartState = CartSharedState()
    
    // StateHoldersの委譲
    val auth by stateHolder<AuthStateHolder>()
    val cart by stateHolder<CartStateHolder>()
    val product by stateHolder<ProductStateHolder>()
}
```

#### StateHolders
```kotlin
// 各機能は独立したStateHolderに
@StateHolder
class AuthStateHolder(/* DI params */) : StateHolder<AuthSource, AuthUiState, AuthAction>(scope), AuthAction {
    // 型定義
    data class AuthSource(/* ... */)
    data class AuthUiState(/* ... */)
    interface AuthAction { /* ... */ }
    
    // 純粋関数で状態変換
    override fun defineState(source: AuthSource): AuthUiState { /* ... */ }
    
    // データソース結合
    override fun createStateFlow(): Flow<AuthSource> { /* ... */ }
    
    // 初期状態
    override fun createInitialState(): AuthUiState { /* ... */ }
    
    // アクション実装
    override val action = this
}

@StateHolder
class CartStateHolder(/* ... */) : StateHolder<CartSource, CartUiState, CartAction>(scope) {
    // 同上: defineState, createStateFlow, createInitialState, action
}

@StateHolder  
class ProductStateHolder(/* ... */) : StateHolder<ProductSource, ProductUiState, ProductAction>(scope) {
    // 同上: defineState, createStateFlow, createInitialState, action
}
```

## StateHolderの動作原理

StateHolderは**Source → State → Action**の循環により状態管理を行います。

### 1. Sourceの生成
`createStateFlow()`メソッドが複数のデータソースを監視し、変更を検知してSourceオブジェクトを生成します。

```kotlin
// Repository、SharedState、ローカル状態を結合
override fun createStateFlow(): Flow<AuthSource> {
    return combine(
        authRepository.observeCurrentUser(),  // 外部データ
        authSharedState.observe(),           // 共有状態
        loadingState                         // ローカル状態
    ) { user, shared, loading ->
        AuthSource(user, shared.isAuthenticated, loading)
    }
}
```

### 2. Stateへの変換
`defineState()`純粋関数がSourceをUI用のStateに変換します。この関数は副作用を持たず、同じSourceに対して常に同じStateを返します。

```kotlin
override fun defineState(source: AuthSource): AuthUiState {
    return AuthUiState(
        displayName = source.user?.name ?: "Guest",
        isAuthenticated = source.isAuthenticated,
        canPerformAction = source.isAuthenticated && !source.isLoading
    )
}
```

### 3. Actionによる変更
UIからActionが実行されると、StateHolderは必要なデータソースを更新します。これがSourceの再生成をトリガーし、新しいStateがUIに流れます。

```kotlin
override suspend fun login(email: String, password: String) {
    loadingState.value = true                // ローカル状態を更新
    val result = authRepository.login(email) // Repositoryを更新  
    loadingState.value = false
    // → 自動的にSourceが再生成され、新しいStateがUIに反映
}
```

### 一方向データフロー
この設計により**Action → データ更新 → Source生成 → State変換 → UI更新**の一方向フローが確立され、状態変更の予測可能性とデバッグの容易性を実現します。

## 技術的制約

### 必須要件

- **Koin**: 依存性注入に必須
- **KSP**: コード生成に必須
- **ViewModelScope**: StateHolderのスコープ管理

### 設計上の制約

- **Dagger/Hilt非対応**: Koinベースの設計
- **SharedStateのmutability**: 変更検知のため意図的にmutable
- **StateHolder間の直接参照禁止**: SharedState経由のみ

## インストール

```bash
git clone https://github.com/Ren-Toyokawa/stateholder-kmp.git
cd stateholder-kmp
./gradlew publishToMavenLocal
```

```kotlin
dependencies {
    implementation("io.github.rentoyokawa:stateholder-annotations:0.1.0")
    implementation("io.github.rentoyokawa:stateholder-core:0.1.0") 
    implementation("io.github.rentoyokawa:stateholder-viewmodel:0.1.0")
    ksp("io.github.rentoyokawa:stateholder-ksp:0.1.0")
    
    implementation("io.insert-koin:koin-core:3.5.6")
    implementation("io.github.rentoyokawa:stateholder-koin:0.1.0")
}
```

## ライセンス

Apache License 2.0

## 著者

**Ren Toyokawa** - [@Ren-Toyokawa](https://github.com/Ren-Toyokawa)