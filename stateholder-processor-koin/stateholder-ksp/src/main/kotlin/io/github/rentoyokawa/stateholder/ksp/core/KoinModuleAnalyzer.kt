package io.github.rentoyokawa.stateholder.ksp.core

import io.github.rentoyokawa.stateholder.ksp.domain.models.StateHolderModel

/**
 * KoinモジュールのanalyzerクラスでStateHolderの解析と生成判定を行う
 */
class KoinModuleAnalyzer {
    
    /**
     * StateHolderに対してKoinモジュールを生成すべきかを判定
     * 
     * @param stateHolder 解析対象のStateHolderModel
     * @return Koinモジュールを生成すべき場合はtrue
     */
    fun shouldGenerateModule(stateHolder: StateHolderModel): Boolean {
        return stateHolder.isInjectable
    }
    
    /**
     * ファクトリー関数に必要なパラメータを決定
     * isInjected=falseのパラメータのみをファクトリーパラメータとして抽出し、
     * 順序を保持してpositionを設定する
     * 
     * @param stateHolder 解析対象のStateHolderModel
     * @return ファクトリーパラメータのリスト
     */
    fun determineFactoryParameters(stateHolder: StateHolderModel): List<FactoryParameter> {
        return stateHolder.constructorParams
            .filterNot { it.isInjected }
            .mapIndexed { index, param ->
                FactoryParameter(
                    name = param.name,
                    type = param.typeName,
                    position = index
                )
            }
    }
}

/**
 * ファクトリー関数のパラメータ情報を表すデータクラス
 * 
 * @property name パラメータ名
 * @property type パラメータの型名
 * @property position パラメータの位置（0ベースのインデックス）
 */
data class FactoryParameter(
    val name: String,
    val type: String,
    val position: Int
)