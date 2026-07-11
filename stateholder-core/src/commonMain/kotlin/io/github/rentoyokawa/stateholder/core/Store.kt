package io.github.rentoyokawa.stateholder.core

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

/**
 * Local（書ける局所状態）の保持・編集と、State 導出を担う抽象基底。
 *
 * 3つの型で責務を分ける:
 * - **Local**: この Store が保持する「書ける真実」。[update] で不変更新できる面はここに正確に一致する。
 * - **Source**: Local と外部データ（Repository / SharedState 等）を [sources] で combine 合成した内部表現。
 * - **State**: Source から [defineState]（純関数）で導出する UI 状態。
 *
 * fold-in（外部データを更新関数で Source に転記する）方式をやめ、外部データは [sources] の combine で
 * 位置的に合成する。これにより「書ける面 = Local」が型で保証され、外部由来フィールドを誤って上書きできない。
 *
 * feature ごとに継承した専用 Store（例: `UserListStore`）を書き、StateHolder がそれを私有物として保持する。
 *
 * 実装上の注意: [state] / 内部 local は `by lazy` で遅延生成する（基底コンストラクタ実行時点では
 * サブクラスの `override val initialLocal` 等が未初期化のため / leaking-this の回避）。
 *
 * @param scope state の共有スコープ（通常は ViewModel のライフタイムに一致するスコープ）
 * @param started state の共有開始ポリシー（既定: WhileSubscribed 5秒）
 */
abstract class Store<Local, Source, State>(
    protected val scope: CoroutineScope,
    private val started: SharingStarted = SharingStarted.WhileSubscribed(DEFAULT_STOP_TIMEOUT_MILLIS),
) {
    /** Local の初期値 */
    protected abstract val initialLocal: Local

    /** state 購読開始前（データ合成前）の初期 UI 状態 */
    protected abstract val initialState: State

    /**
     * Local と外部データを合成して Source を作る。
     * 典型的には `combine(local, repository.xxx, sharedState.flow, ::XxxSource)`。
     */
    protected abstract fun sources(local: Flow<Local>): Flow<Source>

    /** Source から State への純変換 */
    protected abstract fun defineState(source: Source): State

    private val local by lazy { MutableStateFlow(initialLocal) }

    /** 現在の Local（テスト・デバッグ用の読み取り口） */
    val currentLocal: Local get() = local.value

    /** 唯一の書き込み口。Local を不変更新する */
    fun update(transform: (Local) -> Local) {
        local.update(transform)
    }

    /**
     * 導出された UI 状態。
     * 購読中のみ [sources] の合成（外部データの collect）が動き、購読が絶えると停止する。
     * Local 自体は Store が保持し続けるため、再購読しても書き込んだ値は失われない。
     */
    val state: StateFlow<State> by lazy {
        sources(local)
            .map(::defineState)
            .stateIn(scope, started, initialState)
    }

    companion object {
        const val DEFAULT_STOP_TIMEOUT_MILLIS = 5_000L
    }
}
