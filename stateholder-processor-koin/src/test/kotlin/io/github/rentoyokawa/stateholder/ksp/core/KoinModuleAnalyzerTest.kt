package io.github.rentoyokawa.stateholder.ksp.core

import io.github.rentoyokawa.stateholder.ksp.domain.models.ParameterModel
import io.github.rentoyokawa.stateholder.ksp.domain.models.SharedStateModel
import io.github.rentoyokawa.stateholder.ksp.domain.models.StateHolderModel
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class KoinModuleAnalyzerTest {
    private val analyzer = KoinModuleAnalyzer()

    @Test
    fun shouldGenerateModule_InjectableTrue_ReturnsTrue() {
        // Arrange
        val stateHolder = createStateHolderModel(isInjectable = true)
        
        // Act
        val result = analyzer.shouldGenerateModule(stateHolder)
        
        // Assert
        assertTrue(result)
    }
    
    @Test
    fun shouldGenerateModule_InjectableFalse_ReturnsFalse() {
        // Arrange
        val stateHolder = createStateHolderModel(isInjectable = false)
        
        // Act
        val result = analyzer.shouldGenerateModule(stateHolder)
        
        // Assert
        assertFalse(result)
    }
    
    @Test
    fun determineFactoryParameters_NoParameters_ReturnsEmptyList() {
        // Arrange
        val stateHolder = createStateHolderModel(
            constructorParams = emptyList()
        )
        
        // Act
        val result = analyzer.determineFactoryParameters(stateHolder)
        
        // Assert
        assertTrue(result.isEmpty())
    }
    
    @Test
    fun determineFactoryParameters_OnlyInjectedParameters_ReturnsEmptyList() {
        // Arrange
        val params = listOf(
            ParameterModel("userId", "String", isInjected = true, injectionKey = "userId"),
            ParameterModel("userName", "String", isInjected = true, injectionKey = "userName")
        )
        val stateHolder = createStateHolderModel(constructorParams = params)
        
        // Act
        val result = analyzer.determineFactoryParameters(stateHolder)
        
        // Assert
        assertTrue(result.isEmpty())
    }
    
    @Test
    fun determineFactoryParameters_OnlyNonInjectedParameters_ReturnsAllAsFactoryParameters() {
        // Arrange
        val params = listOf(
            ParameterModel("scope", "CoroutineScope", isInjected = false, injectionKey = null),
            ParameterModel("dispatcher", "CoroutineDispatcher", isInjected = false, injectionKey = null)
        )
        val stateHolder = createStateHolderModel(constructorParams = params)
        
        // Act
        val result = analyzer.determineFactoryParameters(stateHolder)
        
        // Assert
        assertEquals(2, result.size)
        assertEquals("scope", result[0].name)
        assertEquals("CoroutineScope", result[0].type)
        assertEquals(0, result[0].position)
        assertEquals("dispatcher", result[1].name)
        assertEquals("CoroutineDispatcher", result[1].type)
        assertEquals(1, result[1].position)
    }
    
    @Test
    fun determineFactoryParameters_MixedParameters_ReturnsOnlyNonInjected() {
        // Arrange
        val params = listOf(
            ParameterModel("userId", "String", isInjected = true, injectionKey = "userId"),
            ParameterModel("scope", "CoroutineScope", isInjected = false, injectionKey = null),
            ParameterModel("config", "Config", isInjected = true, injectionKey = "appConfig"),
            ParameterModel("dispatcher", "CoroutineDispatcher", isInjected = false, injectionKey = null)
        )
        val stateHolder = createStateHolderModel(constructorParams = params)
        
        // Act
        val result = analyzer.determineFactoryParameters(stateHolder)
        
        // Assert
        assertEquals(2, result.size)
        assertEquals("scope", result[0].name)
        assertEquals("CoroutineScope", result[0].type)
        assertEquals(0, result[0].position)
        assertEquals("dispatcher", result[1].name)
        assertEquals("CoroutineDispatcher", result[1].type)
        assertEquals(1, result[1].position)
    }
    
    @Test
    fun determineFactoryParameters_CoroutineScopeParameter_HandledCorrectly() {
        // Arrange
        val params = listOf(
            ParameterModel("scope", "kotlinx.coroutines.CoroutineScope", isInjected = false, injectionKey = null),
            ParameterModel("repository", "UserRepository", isInjected = false, injectionKey = null)
        )
        val stateHolder = createStateHolderModel(constructorParams = params)
        
        // Act
        val result = analyzer.determineFactoryParameters(stateHolder)
        
        // Assert
        assertEquals(2, result.size)
        // scopeパラメータは特別な処理が必要だが、FactoryParameterとしては通常通り扱われる
        assertEquals("scope", result[0].name)
        assertEquals("kotlinx.coroutines.CoroutineScope", result[0].type)
        assertEquals(0, result[0].position)
    }
    
    @Test
    fun determineFactoryParameters_PreservesParameterOrder() {
        // Arrange
        val params = listOf(
            ParameterModel("param1", "Type1", isInjected = false, injectionKey = null),
            ParameterModel("param2", "Type2", isInjected = true, injectionKey = "key2"),
            ParameterModel("param3", "Type3", isInjected = false, injectionKey = null),
            ParameterModel("param4", "Type4", isInjected = false, injectionKey = null),
            ParameterModel("param5", "Type5", isInjected = true, injectionKey = "key5")
        )
        val stateHolder = createStateHolderModel(constructorParams = params)
        
        // Act
        val result = analyzer.determineFactoryParameters(stateHolder)
        
        // Assert
        assertEquals(3, result.size)
        // 順序とpositionが正しく設定されていることを確認
        assertEquals("param1", result[0].name)
        assertEquals(0, result[0].position)
        assertEquals("param3", result[1].name)
        assertEquals(1, result[1].position)
        assertEquals("param4", result[2].name)
        assertEquals(2, result[2].position)
    }
    
    @Test
    fun determineFactoryParameters_ComplexTypes_HandledCorrectly() {
        // Arrange
        val params = listOf(
            ParameterModel("listParam", "List<String>", isInjected = false, injectionKey = null),
            ParameterModel("lambdaParam", "(String) -> Unit", isInjected = false, injectionKey = null),
            ParameterModel("nullableParam", "String?", isInjected = false, injectionKey = null)
        )
        val stateHolder = createStateHolderModel(constructorParams = params)
        
        // Act
        val result = analyzer.determineFactoryParameters(stateHolder)
        
        // Assert
        assertEquals(3, result.size)
        assertEquals("List<String>", result[0].type)
        assertEquals("(String) -> Unit", result[1].type)
        assertEquals("String?", result[2].type)
    }
    
    // Helper function
    private fun createStateHolderModel(
        className: String = "TestStateHolder",
        packageName: String = "com.example.test",
        constructorParams: List<ParameterModel> = emptyList(),
        sharedStates: List<SharedStateModel> = emptyList(),
        isInjectable: Boolean = true
    ): StateHolderModel {
        return StateHolderModel(
            className = className,
            packageName = packageName,
            constructorParams = constructorParams,
            sharedStates = sharedStates,
            isInjectable = isInjectable
        )
    }
}