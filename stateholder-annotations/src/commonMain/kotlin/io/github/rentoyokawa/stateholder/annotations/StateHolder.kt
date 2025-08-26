package io.github.rentoyokawa.stateholder.annotations

import kotlin.annotation.AnnotationRetention
import kotlin.annotation.AnnotationTarget
import kotlin.annotation.Retention
import kotlin.annotation.Target

/**
 * StateHolderクラスを示すアノテーション
 * 
 * このアノテーションが付与されたクラスは、ViewModelのような状態管理クラスとして扱われます。
 * KSPによって処理され、Koinモジュールへの自動登録が行われます。
 * 
 * @param injectable ViewModelへの自動注入を有効にするかどうか（デフォルト: true）
 * 
 * 使用例:
 * ```kotlin
 * @StateHolder(injectable = true)
 * class UserStateHolder(
 *     @InjectedParam("userId") val userId: String
 * ) : StateHolder<UserState>() {
 *     // ...
 * }
 * ```
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class StateHolder(
    val injectable: Boolean = true
)