package io.github.rentoyokawa.stateholder.ksp.domain.models

import com.google.devtools.ksp.symbol.KSFile

/** ViewModel が受け取る1 holder（コンストラクタの named 引数名 + holder 本体） */
data class ViewModelHolderParam(
    val name: String,
    val holder: HolderModel,
)

/**
 * `@StateHolder` 具象 holder をコンストラクタ引数に持つ ViewModel 1つ分のモデル。
 *
 * [holderParams] の並びはコンストラクタでの宣言順（位置）をそのまま保持する。
 */
data class ViewModelModel(
    val qualifiedName: String,
    val simpleName: String,
    val packageName: String,
    val holderParams: List<ViewModelHolderParam>,
    val containingFile: KSFile?,
)
