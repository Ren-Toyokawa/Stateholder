package io.github.rentoyokawa.stateholder.ksp.domain.models

/**
 * ViewModelの情報を表すドメインモデル
 * ViewModelクラスの情報と注入されるStateHolder、共有状態を保持
 */
data class ViewModelModel(
    val className: String,
    val packageName: String,
    val injectedStateHolders: List<InjectedStateHolderModel>,
    val sharedStates: List<SharedStateModel>
)

/**
 * 注入されるStateHolderの情報を表すドメインモデル
 * @InjectStateHolderアノテーションが付いたプロパティの情報を保持
 */
data class InjectedStateHolderModel(
    val propertyName: String,
    val typeName: String
)