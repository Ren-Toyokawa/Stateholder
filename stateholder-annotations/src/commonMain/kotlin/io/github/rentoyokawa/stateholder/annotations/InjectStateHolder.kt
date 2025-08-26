package io.github.rentoyokawa.stateholder.annotations

import kotlin.annotation.AnnotationRetention
import kotlin.annotation.AnnotationTarget
import kotlin.annotation.Retention
import kotlin.annotation.Target

/**
 * StateHolderの注入を示すアノテーション
 * 
 * このアノテーションが付与されたパラメータには、対応するStateHolderインスタンスが
 * 自動的に注入されます。主にViewクラスのコンストラクタパラメータで使用されます。
 * 
 * 使用例:
 * ```kotlin
 * class UserView(
 *     @InjectStateHolder
 *     private val userStateHolder: UserStateHolder
 * ) {
 *     // userStateHolderを使用したView実装
 * }
 * ```
 */
@Target(AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class InjectStateHolder