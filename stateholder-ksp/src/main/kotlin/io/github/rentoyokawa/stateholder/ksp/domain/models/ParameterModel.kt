package io.github.rentoyokawa.stateholder.ksp.domain.models

/**
 * コンストラクタパラメータの情報を表すドメインモデル
 * StateHolderクラスのコンストラクタ引数を表現
 */
data class ParameterModel(
    val name: String,
    val typeName: String,
    val isInjected: Boolean,
    val injectionKey: String?
)