package io.github.rentoyokawa.stateholder.extensions

import androidx.lifecycle.ViewModel
import io.github.rentoyokawa.stateholder.delegate.StateHolderDelegate
import org.koin.core.qualifier.Qualifier

/**
 * ViewModelでStateHolderを簡潔に宣言するための拡張関数
 * 
 * この関数は型推論により自動的にStateHolderの型を判定し、
 * Koinファクトリー内でViewModelインスタンスからSharedStateを動的に解決します。
 * 
 * 使用例:
 * ```kotlin
 * class MyViewModel : ViewModel() {
 *     @SharedState
 *     internal val userState = UserSharedState()
 *     
 *     // 型推論のみで自動解決！
 *     val userStateHolder: UserStateHolder by stateHolder()
 * }
 * ```
 * 
 * この拡張関数により、以下の機能が自動的に提供されます：
 * - Koinからの自動解決
 * - viewModelScopeの自動渡し
 * - Koinファクトリー内でのSharedStateの動的解決
 * 
 * @param T StateHolderの型（型推論により自動的に決定）
 * @param qualifier Koinのqualifier（オプション）
 * @return StateHolderのデリゲート
 */
inline fun <reified T : Any> ViewModel.stateHolder(
    qualifier: Qualifier? = null
): StateHolderDelegate<T> {
    return StateHolderDelegate(
        viewModel = this,
        qualifier = qualifier,
        clazz = T::class
    )
}