package io.github.rentoyokawa.stateholder.delegate

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import org.koin.core.component.KoinComponent
import org.koin.core.parameter.parametersOf
import org.koin.core.qualifier.Qualifier
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

/**
 * StateHolderをViewModelにデリゲートするためのプロパティデリゲート
 * 
 * このデリゲートは以下の機能を提供します：
 * - ViewModelからStateHolderを遅延初期化で取得
 * - viewModelScopeを自動的にStateHolderに渡す
 * - Koinからの自動解決
 * 
 * @param T StateHolderの型
 * @param viewModel StateHolderを使用するViewModel
 * @param qualifier Koinのqualifier（オプション）
 * @param clazz StateHolderのKClass
 */
class StateHolderDelegate<T : Any>(
    private val viewModel: ViewModel,
    private val qualifier: Qualifier? = null,
    private val clazz: KClass<T>
) : ReadOnlyProperty<ViewModel, T> {

    private var stateHolder: T? = null

    override fun getValue(thisRef: ViewModel, property: KProperty<*>): T {
        // 既に初期化済みならそれを返す
        stateHolder?.let { return it }
        
        // 初期化されていない場合は作成
        return createStateHolder().also { stateHolder = it }
    }

    private fun createStateHolder(): T {
        // ViewModelがKoinComponentを実装していることを前提とする
        if (viewModel !is KoinComponent) {
            throw IllegalStateException("ViewModel must implement KoinComponent to use stateHolder delegate")
        }
        
        // KoinにViewModelインスタンスとviewModelScopeをパラメータとして渡す
        // Koinファクトリー内でViewModelからSharedStateを動的に取得する
        return viewModel.getKoin().get(
            clazz = clazz,
            qualifier = qualifier,
            parameters = { parametersOf(viewModel, viewModel.viewModelScope) }
        )
    }
}