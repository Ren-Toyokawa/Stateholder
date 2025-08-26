package io.github.rentoyokawa.stateholder.ksp.domain.models

/**
 * 共有状態プロパティの情報を表すドメインモデル
 * @SharedStateアノテーションが付いたプロパティの情報を保持
 */
data class SharedStateModel(
    val propertyName: String,
    val typeName: String,
    val isMutable: Boolean
)