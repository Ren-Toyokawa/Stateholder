package io.github.rentoyokawa.stateholder.annotations

import kotlin.annotation.AnnotationRetention
import kotlin.annotation.AnnotationTarget
import kotlin.annotation.Retention
import kotlin.annotation.Target

/**
 * StateHolderのコンストラクタパラメータへの注入を示すアノテーション
 * 
 * このアノテーションが付与されたパラメータは、StateHolderインスタンス生成時に
 * 指定されたキーを使用して値が注入されます。
 * 
 * @property key パラメータを識別するためのキー（空文字列やブランクは推奨されません）
 * 
 * 注意: キーの妥当性検証はKSPプロセッサで行われます。
 * 
 * 使用例:
 * ```kotlin
 * @StateHolder
 * class UserStateHolder(
 *     @InjectedParam("userId")
 *     private val userId: String,
 *     @InjectedParam("userName")
 *     private val userName: String
 * ) : StateHolder<UserState>() {
 *     // ...
 * }
 * ```
 */
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class InjectedParam(
    val key: String
)