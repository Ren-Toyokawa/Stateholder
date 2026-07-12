package io.github.rentoyokawa.stateholder.ksp.domain.models

import com.google.devtools.ksp.symbol.KSFile

/** holder コンストラクタの1引数（名前 + 分類） */
data class HolderParam(
    val name: String,
    val kind: ParamKind,
)

/**
 * `@StateHolder` が付いた具象 holder クラス1つ分のモデル。
 *
 * KSP のシンボル（[com.google.devtools.ksp.symbol.KSClassDeclaration]）から検出時に変換して作る。
 * 以降の検出・生成ロジックは KSP 型に直接触れず、このモデルだけを扱う。
 */
data class HolderModel(
    val qualifiedName: String,
    val simpleName: String,
    val packageName: String,
    val params: List<HolderParam>,
    /** インクリメンタルコンパイルの依存追跡用（[com.google.devtools.ksp.processing.Dependencies] に渡す） */
    val containingFile: KSFile?,
)
