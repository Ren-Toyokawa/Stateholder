package io.github.rentoyokawa.stateholder.ksp.domain.models

/**
 * StateHolderクラスの情報を表すドメインモデル
 * KSP APIから独立した純粋なデータクラス
 */
data class StateHolderModel(
    val className: String,
    val packageName: String,
    val constructorParams: List<ParameterModel>,
    val sharedStates: List<SharedStateModel>,
    val isInjectable: Boolean
)