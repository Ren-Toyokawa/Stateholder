package io.github.rentoyokawa.stateholder.annotations

import kotlin.annotation.AnnotationRetention
import kotlin.annotation.AnnotationTarget
import kotlin.annotation.Retention
import kotlin.annotation.Target

/**
 * 共有状態プロパティを示すアノテーション
 * 
 * このアノテーションが付与されたプロパティは、StateHolder間で共有される状態として扱われます。
 * 複数のStateHolderインスタンス間で同じ状態を参照する必要がある場合に使用します。
 * 
 * 使用例:
 * ```kotlin
 * @StateHolder
 * class UserStateHolder {
 *     @SharedState
 *     val userPreferences = UserPreferences()
 * }
 * ```
 */
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class SharedState