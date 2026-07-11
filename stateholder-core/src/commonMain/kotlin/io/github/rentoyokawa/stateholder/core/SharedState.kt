package io.github.rentoyokawa.stateholder.core

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * ViewModel スコープの軽量な共有ストア。
 *
 * 複数の StateHolder 間で id などの軽量な情報を共有するためのもの。
 * 重い状態を置く場所ではない（それは各 StateHolder の Source の役割）。
 * ニュアンスは Android の SavedStateHandle に近い（VM スコープ・軽量・観測可能）が、
 * 永続性（プロセス死からの復元）は持たない。
 *
 * 典型例: 一覧側の StateHolder が [set] で選択 id を書き、
 * 詳細側の StateHolder が [flow] を Store の入力として購読して追従する。
 */
class SharedState<T>(initial: T) {
    private val _flow = MutableStateFlow(initial)

    /** 購読口。[Store] の sources() の combine 入力として渡す */
    val flow: StateFlow<T> = _flow.asStateFlow()

    /** 現在値 */
    val value: T get() = _flow.value

    /** 値を置き換える */
    fun set(value: T) {
        _flow.value = value
    }

    /** 現在値から新しい値を導出して更新する */
    fun update(transform: (T) -> T) {
        _flow.update(transform)
    }
}
