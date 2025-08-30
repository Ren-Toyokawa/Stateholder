
package io.github.rentoyokawa.stateholder.ksp.generator

import io.github.rentoyokawa.stateholder.ksp.core.StateHolderAnalysis
import io.github.rentoyokawa.stateholder.ksp.domain.models.ParameterModel
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class KoinModuleGeneratorTest {
    
    private val generator = KoinModuleGenerator()
    
    @Test
    fun `generateModule with single StateHolder - dynamic mapping`() {
        // Arrange
        val analysis = StateHolderAnalysis(
            moduleName = "UserModule",
            className = "io.example.UserStateHolder",
            factoryParams = listOf(
                ParameterModel(
                    name = "userState",
                    typeName = "UserSharedState",
                    isInjected = true,
                    injectionKey = null
                ),
                ParameterModel(
                    name = "scope",
                    typeName = "CoroutineScope",
                    isInjected = false,
                    injectionKey = null
                )
            ),
            shouldGenerate = true
        )
        
        // Act
        val result = generator.generateModule(listOf(analysis))
        
        // Assert - 動的マッピング方式に対応
        assertTrue(result.contains("val stateHolderModule = module"), "Expected to contain 'val stateHolderModule = module'")
        assertTrue(result.contains("factory<io.example.UserStateHolder>"))
        assertTrue(result.contains("params ->"))
        assertTrue(result.contains("val viewModel = params.get<androidx.lifecycle.ViewModel>(0)"))
        assertTrue(result.contains("val scope = params.get<kotlinx.coroutines.CoroutineScope>(1)"))
    }
    
    @Test
    fun `generateModule with multiple StateHolders`() {
        // Arrange
        val analyses = listOf(
            StateHolderAnalysis(
                moduleName = "UserModule",
                className = "io.example.UserStateHolder",
                factoryParams = listOf(
                    ParameterModel(
                        name = "userState",
                        typeName = "UserSharedState",
                        isInjected = true,
                        injectionKey = null
                    ),
                    ParameterModel(
                        name = "scope",
                        typeName = "CoroutineScope",
                        isInjected = false,
                        injectionKey = null
                    )
                ),
                shouldGenerate = true
            ),
            StateHolderAnalysis(
                moduleName = "DashboardModule",
                className = "io.example.DashboardStateHolder",
                factoryParams = listOf(
                    ParameterModel(
                        name = "scope",
                        typeName = "CoroutineScope",
                        isInjected = false,
                        injectionKey = null
                    ),
                    ParameterModel(
                        name = "dispatcher",
                        typeName = "CoroutineDispatcher",
                        isInjected = false,
                        injectionKey = null
                    )
                ),
                shouldGenerate = true
            )
        )
        
        // Act
        val result = generator.generateModule(analyses)
        
        // Assert - 動的マッピングに対応
        assertTrue(result.contains("val stateHolderModule = module"))
        assertTrue(result.contains("factory<io.example.UserStateHolder>"))
        assertTrue(result.contains("factory<io.example.DashboardStateHolder>"))
        assertTrue(result.contains("params ->"))
    }
    
    @Test
    fun `generateModule with empty list returns empty module`() {
        // Arrange
        val analyses = emptyList<StateHolderAnalysis>()
        
        // Act
        val result = generator.generateModule(analyses)
        
        // Assert - KotlinPoetは空モジュールを "module {\n}" とフォーマットする
        val normalizedResult = result.replace("\\s+".toRegex(), " ").trim()
        val normalizedExpected = "val stateHolderModule = module { }".trim()
        
        assertEquals(normalizedExpected, normalizedResult)
    }
    
    @Test
    fun `generateModule filters out StateHolders with shouldGenerate false`() {
        // Arrange
        val analyses = listOf(
            StateHolderAnalysis(
                moduleName = "UserModule",
                className = "io.example.UserStateHolder",
                factoryParams = emptyList(),
                shouldGenerate = true
            ),
            StateHolderAnalysis(
                moduleName = "AdminModule",
                className = "io.example.AdminStateHolder",
                factoryParams = emptyList(),
                shouldGenerate = false
            )
        )
        
        // Act
        val result = generator.generateModule(analyses)
        
        // Assert
        assertTrue(result.contains("io.example.UserStateHolder"))
        assertFalse(result.contains("io.example.AdminStateHolder"))
    }
    
    @Test
    fun `generateModule with no factory params`() {
        // Arrange
        val analysis = StateHolderAnalysis(
            moduleName = "SimpleModule",
            className = "io.example.SimpleStateHolder",
            factoryParams = emptyList(),
            shouldGenerate = true
        )
        
        // Act
        val result = generator.generateModule(listOf(analysis))
        
        // Assert - 動的マッピング方式でも空パラメータに対応
        assertTrue(result.contains("val stateHolderModule = module"))
        assertTrue(result.contains("factory<io.example.SimpleStateHolder>"))
        assertTrue(result.contains("params ->"))
        assertTrue(result.contains("io.example.SimpleStateHolder()") || result.contains("io.example.SimpleStateHolder()\n"))
    }
    
    @Test
    fun `generateModule with Koin dependency injection`() {
        // Arrange
        val analysis = StateHolderAnalysis(
            moduleName = "ComplexModule",
            className = "io.example.ComplexStateHolder",
            factoryParams = listOf(
                ParameterModel(
                    name = "repository",
                    typeName = "UserRepository",
                    isInjected = false,
                    injectionKey = null
                ),
                ParameterModel(
                    name = "scope",
                    typeName = "CoroutineScope",
                    isInjected = false,
                    injectionKey = null
                )
            ),
            shouldGenerate = true
        )
        
        // Act
        val result = generator.generateModule(listOf(analysis))
        
        // Assert - Koinからの依存性注入が正しく処理されることを確認
        assertTrue(result.contains("factory<io.example.ComplexStateHolder>"))
        assertTrue(result.contains("params ->"))
        assertTrue(result.contains("repository = get()") || result.contains("get()"))
    }
    
    @Test
    fun `generateModule with generic parameter type`() {
        // Arrange
        val analysis = StateHolderAnalysis(
            moduleName = "GenericModule",
            className = "io.example.GenericStateHolder",
            factoryParams = listOf(
                ParameterModel(
                    name = "items",
                    typeName = "List<Item>",
                    isInjected = false,
                    injectionKey = null
                ),
                ParameterModel(
                    name = "scope",
                    typeName = "CoroutineScope",
                    isInjected = false,
                    injectionKey = null
                )
            ),
            shouldGenerate = true
        )
        
        // Act
        val result = generator.generateModule(listOf(analysis))
        
        // Assert - 動的マッピング方式でジェネリクス型もKoinから取得
        assertTrue(result.contains("factory<io.example.GenericStateHolder>"))
        assertTrue(result.contains("params ->"))
        assertTrue(result.contains("items = get()") || result.contains("get()"))
    }
    
    @Test
    fun `generateModule with both lambda and generic parameter types`() {
        // Arrange
        val analysis = StateHolderAnalysis(
            moduleName = "ComplexModule",
            className = "io.example.ComplexStateHolder",
            factoryParams = listOf(
                ParameterModel(
                    name = "callback",
                    typeName = "(String) -> Unit",
                    isInjected = false,
                    injectionKey = null
                ),
                ParameterModel(
                    name = "items",
                    typeName = "List<Item>",
                    isInjected = false,
                    injectionKey = null
                ),
                ParameterModel(
                    name = "scope",
                    typeName = "CoroutineScope",
                    isInjected = false,
                    injectionKey = null
                )
            ),
            shouldGenerate = true
        )
        
        // Act
        val result = generator.generateModule(listOf(analysis))
        
        // Assert - 動的マッピング方式で複雑な型もKoinから取得
        assertTrue(result.contains("factory<io.example.ComplexStateHolder>"))
        assertTrue(result.contains("params ->"))
        assertTrue(result.contains("get()"))
    }
}