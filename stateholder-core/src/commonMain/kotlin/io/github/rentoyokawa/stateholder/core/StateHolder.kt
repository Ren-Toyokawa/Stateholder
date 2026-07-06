package io.github.rentoyokawa.stateholder.core

import kotlinx.coroutines.flow.StateFlow

/**
 * 消費側（View / ViewModel）が見る唯一の契約。
 *
 * 読み = [state] / 書き = [action] の対称な窓口を提供する。
 * Source（内部表現）はこの契約に現れない。実装の詳細は [Store] に委ねる。
 *
 * @param State View に公開する UI 状態
 * @param Action View から呼べる操作の契約（利用側が interface として定義する）
 */
interface StateHolder<State, Action> {
    val state: StateFlow<State>
    val action: Action
}
