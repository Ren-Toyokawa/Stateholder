package io.github.rentoyokawa.stateholder.ksp

import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider

/**
 * StateHolderProcessorのプロバイダークラス
 * 
 * KSPによってStateHolderProcessorのインスタンスを生成するためのプロバイダーです。
 * このクラスはKSPフレームワークによって自動的に呼び出され、
 * 実際のアノテーション処理を行うStateHolderProcessorインスタンスを提供します。
 */
class StateHolderProcessorProvider : SymbolProcessorProvider {

    /**
     * StateHolderProcessorのインスタンスを作成
     * 
     * @param environment KSPの実行環境情報（ロガー、コードジェネレーター等を含む）
     * @return 設定されたStateHolderProcessorインスタンス
     */
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        environment.logger.info("StateHolderProcessorProvider: StateHolderProcessorを作成します")
        environment.logger.info("StateHolderProcessorProvider: オプション - ${environment.options}")
        
        return StateHolderProcessor(
            codeGenerator = environment.codeGenerator,
            logger = environment.logger,
            options = environment.options
        ).also {
            environment.logger.info("StateHolderProcessorProvider: StateHolderProcessorの作成が完了しました")
        }
    }
}