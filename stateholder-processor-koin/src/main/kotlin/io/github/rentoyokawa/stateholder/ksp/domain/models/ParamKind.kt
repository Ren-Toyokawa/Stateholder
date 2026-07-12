package io.github.rentoyokawa.stateholder.ksp.domain.models

import com.squareup.kotlinpoet.TypeName

/**
 * holder コンストラクタ引数の分類（型ベース）。
 *
 * [[STA-13]] の新設計では引数名や追加アノテーションではなく「型」だけで結線方法が決まる:
 * `SharedState<T>` → VM 内で T ごとに 1 インスタンスを共有、`CoroutineScope` → VM ごとの共有 scope、
 * それ以外 → Koin の `get()` で解決。
 */
sealed interface ParamKind {
    /**
     * 引数型が `SharedState<T>` であることを示す。
     *
     * @param typeArg T の KotlinPoet 表現（nullable 情報を含む）。同一 VM 内で同じ [typeArg] を
     *   要求する複数 holder には同一インスタンスが渡される。
     */
    data class Shared(val typeArg: TypeName) : ParamKind

    /** 引数型が `kotlinx.coroutines.CoroutineScope` であることを示す */
    data object Scope : ParamKind

    /** 上記以外。Koin の `get()` で解決する対象 */
    data object Resolved : ParamKind
}
