package io.github.rentoyokawa.stateholder.ksp.core

import com.google.devtools.ksp.processing.KSPLogger
import io.github.rentoyokawa.stateholder.ksp.domain.models.HolderModel
import io.github.rentoyokawa.stateholder.ksp.domain.models.ParamKind

/**
 * [HolderModel] の生成前バリデーション。
 *
 * `SharedState<T>(initial)` は初期値必須だが、KSP は holder 引数の型 `SharedState<T>` しか知らない。
 * T が nullable なら生成器は `null` を初期値として使えるが、非 null 型は初期値を機械的に決められないため
 * ここでエラーとして明示する（[[STA-14]] 計画の暫定方針）。
 */
class HolderValidator(private val logger: KSPLogger) {

    /** 検証エラーが1件でもあれば true を返す（呼び出し側は生成をスキップすること） */
    fun validate(holders: List<HolderModel>): Boolean {
        var hasError = false
        holders.forEach { holder ->
            holder.params.forEach { param ->
                val kind = param.kind
                if (kind is ParamKind.Shared && !kind.typeArg.isNullable) {
                    logger.error(
                        "[ksp] ${holder.qualifiedName} の引数 '${param.name}': " +
                            "SharedState<${kind.typeArg}> は非nullable型です。" +
                            "KSPは初期値を機械的に決定できないため、SharedStateの型引数はnullableにしてください " +
                            "（例: SharedState<${kind.typeArg}?>）。",
                    )
                    hasError = true
                }
            }
        }
        return hasError
    }
}
