package io.github.rentoyokawa.stateholder.core

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Source の保持・編集・State 導出を一手に担うコンテナ。
 *
 * - Source はこの Store が保持する「単一の真実」。
 * - 書き込み口は [update] ただ1つ。ユーザー操作（Action）も外部データの到着（[inputs]）も、
 *   すべて「Source への更新関数」に正規化される。
 * - [state] は Source から [defineState]（純関数）で導出される。
 * - [inputs] の購読は [state] が購読されている間だけ行われる（WhileSubscribed セマンティクス）。
 *
 * StateHolder 実装はこの Store を私有物として保持し、[state] を公開・Action 実装から [update] を呼ぶ。
 *
 * @param initial Source の初期値
 * @param scope state の共有スコープ（通常は ViewModel のライフタイムに一致するスコープ）
 * @param inputs 外部入力（SharedState / Repository 等）を「Source への更新関数」として合流させる Flow 群。
 *               [input] ヘルパーで生成できる
 * @param started state の共有開始ポリシー（既定: WhileSubscribed 5秒）
 * @param defineState Source から State への純変換
 */
class Store<Source, State>(
    initial: Source,
    scope: CoroutineScope,
    inputs: List<Flow<(Source) -> Source>> = emptyList(),
    started: SharingStarted = SharingStarted.WhileSubscribed(DEFAULT_STOP_TIMEOUT_MILLIS),
    private val defineState: (Source) -> State,
) {
    private val source = MutableStateFlow(initial)

    /** 現在の Source（テスト・デバッグ用の読み取り口） */
    val currentSource: Source get() = source.value

    /** 唯一の書き込み口。Source を不変更新する */
    fun update(transform: (Source) -> Source) {
        source.update(transform)
    }

    /**
     * 導出された UI 状態。
     * 購読が始まると [inputs] の合流も開始し、購読が絶えると（タイムアウト後）停止する。
     * Source 自体は Store が保持し続けるため、再購読しても値は失われない。
     */
    val state: StateFlow<State> = flow {
        coroutineScope {
            inputs.forEach { input ->
                launch { input.collect(::update) }
            }
            emitAll(source)
        }
    }
        .map(defineState)
        .stateIn(scope, started, defineState(initial))

    companion object {
        const val DEFAULT_STOP_TIMEOUT_MILLIS = 5_000L
    }
}

/**
 * 外部の [Flow] を Store の入力（Source への更新関数の列）へ変換するヘルパー。
 *
 * ```
 * inputs = listOf(
 *     input(repository.users) { source, users -> source.copy(users = users) },
 *     input(selectedId.flow) { source, id -> source.copy(selectedId = id) },
 * )
 * ```
 */
fun <Source, T> input(flow: Flow<T>, fold: (Source, T) -> Source): Flow<(Source) -> Source> =
    flow.map { value -> { source: Source -> fold(source, value) } }
