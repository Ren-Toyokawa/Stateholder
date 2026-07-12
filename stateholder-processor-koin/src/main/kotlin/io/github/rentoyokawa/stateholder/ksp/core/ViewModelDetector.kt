package io.github.rentoyokawa.stateholder.ksp.core

import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import io.github.rentoyokawa.stateholder.ksp.domain.models.HolderModel
import io.github.rentoyokawa.stateholder.ksp.domain.models.ViewModelHolderParam
import io.github.rentoyokawa.stateholder.ksp.domain.models.ViewModelModel

/**
 * 「コンストラクタ引数の型が `@StateHolder` 具象クラスであるクラス」を ViewModel として検出する。
 *
 * [[STA-13]] の検出条件どおり、`androidx.lifecycle.ViewModel` の継承は要求しない
 * （`viewModel {}` DSL を使う場合のみ継承が必要になるが、それは消費側フィクスチャの都合であり
 * 検出条件そのものではない）。
 */
class ViewModelDetector(private val resolver: Resolver) {

    /**
     * リポジトリ内の全ファイルの宣言を走査して ViewModel を検出する（qualifiedName の辞書順）。
     */
    fun detectViewModels(holders: List<HolderModel>): List<ViewModelModel> {
        if (holders.isEmpty()) return emptyList()

        val holderByQualifiedName = holders.associateBy { it.qualifiedName }

        val viewModels = mutableListOf<ViewModelModel>()
        resolver.getAllFiles().forEach { file ->
            file.declarations
                .filterIsInstance<KSClassDeclaration>()
                .filter { it.classKind == ClassKind.CLASS }
                .forEach { declaration ->
                    toViewModelModelOrNull(declaration, holderByQualifiedName)?.let { viewModels.add(it) }
                }
        }

        return viewModels.sortedBy { it.qualifiedName }
    }

    private fun toViewModelModelOrNull(
        declaration: KSClassDeclaration,
        holderByQualifiedName: Map<String, HolderModel>,
    ): ViewModelModel? {
        val primaryConstructor = declaration.primaryConstructor ?: return null

        val holderParams = primaryConstructor.parameters.mapNotNull { param ->
            val paramName = param.name?.asString() ?: return@mapNotNull null
            val qualifiedName = param.type.resolve().declaration.qualifiedName?.asString() ?: return@mapNotNull null
            val holder = holderByQualifiedName[qualifiedName] ?: return@mapNotNull null
            ViewModelHolderParam(name = paramName, holder = holder)
        }

        if (holderParams.isEmpty()) return null

        val qualifiedName = declaration.qualifiedName?.asString() ?: return null

        return ViewModelModel(
            qualifiedName = qualifiedName,
            simpleName = declaration.simpleName.asString(),
            packageName = declaration.packageName.asString(),
            holderParams = holderParams,
            containingFile = declaration.containingFile,
        )
    }
}
