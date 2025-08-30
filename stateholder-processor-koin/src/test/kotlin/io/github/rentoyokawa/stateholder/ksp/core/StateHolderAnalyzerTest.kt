package io.github.rentoyokawa.stateholder.ksp.core

import io.github.rentoyokawa.stateholder.ksp.domain.models.ParameterModel
import io.github.rentoyokawa.stateholder.ksp.domain.models.SharedStateModel
import io.github.rentoyokawa.stateholder.ksp.domain.models.StateHolderModel
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class StateHolderAnalyzerTest {
    private val analyzer = StateHolderAnalyzer()

    companion object {
        private const val DEFAULT_CLASS_NAME = "TestStateHolder"
        private const val DEFAULT_PACKAGE = "com.example.test"
        private const val USER_STATE_HOLDER = "UserStateHolder"
        private const val COMPLEX_STATE_HOLDER = "ComplexStateHolder"
    }

    // region shouldGenerate determination tests
    
    @Test
    fun analyzeStateHolder_InjectableTrue_ShouldGenerate() {
        // Arrange
        val model = createStateHolderModel(isInjectable = true)
        
        // Act
        val result = analyzer.analyzeStateHolder(model)
        
        // Assert
        assertTrue(result.shouldGenerate)
    }
    
    @Test
    fun analyzeStateHolder_InjectableFalse_ShouldNotGenerate() {
        // Arrange
        val model = createStateHolderModel(isInjectable = false)
        
        // Act
        val result = analyzer.analyzeStateHolder(model)
        
        // Assert
        assertFalse(result.shouldGenerate)
    }
    
    // endregion
    
    // region factoryParams extraction tests
    
    @Test
    fun extractFactoryParams_MixedInjectedAndNonInjectedParams_ReturnsOnlyNonInjected() {
        // Arrange
        val injectedParam1 = ParameterModel(
            name = "userId",
            typeName = "String",
            isInjected = true,
            injectionKey = "userId"
        )
        val injectedParam2 = ParameterModel(
            name = "userName",
            typeName = "String",
            isInjected = true,
            injectionKey = "userName"
        )
        val nonInjectedParam1 = ParameterModel(
            name = "scope",
            typeName = "CoroutineScope",
            isInjected = false,
            injectionKey = null
        )
        val nonInjectedParam2 = ParameterModel(
            name = "dispatcher",
            typeName = "CoroutineDispatcher",
            isInjected = false,
            injectionKey = null
        )
        
        val model = createStateHolderModel(
            constructorParams = listOf(
                injectedParam1,
                nonInjectedParam1,
                injectedParam2,
                nonInjectedParam2
            )
        )
        
        // Act
        val result = analyzer.analyzeStateHolder(model)
        
        // Assert
        val expectedFactoryParams = listOf(nonInjectedParam1, nonInjectedParam2)
        assertEquals(expectedFactoryParams, result.factoryParams)
    }
    
    @Test
    fun extractFactoryParams_AllParametersInjected_ReturnsEmpty() {
        // Arrange
        val allInjectedParams = listOf(
            ParameterModel("param1", "Type1", isInjected = true, injectionKey = "key1"),
            ParameterModel("param2", "Type2", isInjected = true, injectionKey = "key2")
        )
        val model = createStateHolderModel(constructorParams = allInjectedParams)
        
        // Act
        val result = analyzer.analyzeStateHolder(model)
        
        // Assert
        assertTrue(result.factoryParams.isEmpty())
    }
    
    @Test
    fun extractFactoryParams_NoParametersInjected_ReturnsAll() {
        // Arrange
        val allNonInjectedParams = listOf(
            ParameterModel("param1", "Type1", isInjected = false, injectionKey = null),
            ParameterModel("param2", "Type2", isInjected = false, injectionKey = null)
        )
        val model = createStateHolderModel(constructorParams = allNonInjectedParams)
        
        // Act
        val result = analyzer.analyzeStateHolder(model)
        
        // Assert
        assertEquals(allNonInjectedParams, result.factoryParams)
    }
    
    @Test
    fun extractFactoryParams_MultipleNonInjectedParams_PreservesOrder() {
        // Arrange
        val params = listOf(
            ParameterModel("param1", "Type1", isInjected = false, injectionKey = null),
            ParameterModel("param2", "Type2", isInjected = true, injectionKey = "key2"),
            ParameterModel("param3", "Type3", isInjected = false, injectionKey = null),
            ParameterModel("param4", "Type4", isInjected = false, injectionKey = null)
        )
        val model = createStateHolderModel(constructorParams = params)
        
        // Act
        val result = analyzer.analyzeStateHolder(model)
        
        // Assert
        assertEquals(3, result.factoryParams.size)
        assertEquals("param1", result.factoryParams[0].name)
        assertEquals("param3", result.factoryParams[1].name)
        assertEquals("param4", result.factoryParams[2].name)
    }
    
    @Test
    fun extractFactoryParams_EmptyConstructorParams_ReturnsEmpty() {
        // Arrange
        val model = createStateHolderModel(constructorParams = emptyList())
        
        // Act
        val result = analyzer.analyzeStateHolder(model)
        
        // Assert
        assertTrue(result.factoryParams.isEmpty())
    }
    
    // endregion
    
    // region moduleName generation tests
    
    @Test
    fun generateModuleName_ClassWithStateHolderSuffix_RemovesSuffixAndAddsModule() {
        // Arrange
        val model = createStateHolderModel(className = USER_STATE_HOLDER)
        
        // Act
        val result = analyzer.analyzeStateHolder(model)
        
        // Assert
        assertEquals("UserModule", result.moduleName)
    }
    
    @Test
    fun generateModuleName_ClassWithoutStateHolderSuffix_AddsModuleSuffix() {
        // Arrange
        val model = createStateHolderModel(className = "CustomHolder")
        
        // Act
        val result = analyzer.analyzeStateHolder(model)
        
        // Assert
        assertEquals("CustomHolderModule", result.moduleName)
    }
    
    @Test
    fun generateModuleName_StateHolderOnlyClassName_ReturnsModule() {
        // Arrange
        val model = createStateHolderModel(className = "StateHolder")
        
        // Act
        val result = analyzer.analyzeStateHolder(model)
        
        // Assert
        assertEquals("Module", result.moduleName)
    }
    
    @Test
    fun generateModuleName_MultipleStateHolderSuffixes_RemovesOnlyLastSuffix() {
        // Arrange
        val model = createStateHolderModel(className = "StateHolderStateHolder")
        
        // Act
        val result = analyzer.analyzeStateHolder(model)
        
        // Assert
        assertEquals("StateHolderModule", result.moduleName)
    }
    
    // endregion
    
    // region className preservation tests
    
    @Test
    fun analyzeStateHolder_AnyClassName_PreservesOriginalClassName() {
        // Arrange
        val originalClassName = "MyCustomStateHolder"
        val model = createStateHolderModel(className = originalClassName)
        
        // Act
        val result = analyzer.analyzeStateHolder(model)
        
        // Assert
        assertEquals(originalClassName, result.className)
    }
    
    @Test
    fun generateModuleName_ClassNameWithSpecialCharacters_HandlesCorrectly() {
        // Arrange
        val model = createStateHolderModel(className = "User\$StateHolder")
        
        // Act
        val result = analyzer.analyzeStateHolder(model)
        
        // Assert
        assertEquals("User\$Module", result.moduleName)
    }
    
    @Test
    fun generateModuleName_VeryLongClassName_HandlesCorrectly() {
        // Arrange
        val longClassName = "VeryLongClassNameThatExceedsNormalLengthExpectationsForTestingPurposesStateHolder"
        val model = createStateHolderModel(className = longClassName)
        
        // Act
        val result = analyzer.analyzeStateHolder(model)
        
        // Assert
        val expectedModuleName = "VeryLongClassNameThatExceedsNormalLengthExpectationsForTestingPurposesModule"
        assertEquals(expectedModuleName, result.moduleName)
    }
    
    @Test
    fun generateModuleName_EmptyClassName_ReturnsModule() {
        // Arrange
        val model = createStateHolderModel(className = "")
        
        // Act
        val result = analyzer.analyzeStateHolder(model)
        
        // Assert
        assertEquals("Module", result.moduleName)
    }
    
    @Test
    fun extractFactoryParams_ParametersWithComplexTypes_HandlesCorrectly() {
        // Arrange
        val complexParams = listOf(
            ParameterModel("genericParam", "List<String>", isInjected = false, injectionKey = null),
            ParameterModel("lambdaParam", "(String) -> Unit", isInjected = false, injectionKey = null),
            ParameterModel("nullableParam", "String?", isInjected = false, injectionKey = null)
        )
        val model = createStateHolderModel(constructorParams = complexParams)
        
        // Act
        val result = analyzer.analyzeStateHolder(model)
        
        // Assert
        assertEquals(complexParams, result.factoryParams)
    }
    
    // endregion
    
    // region Integration tests
    
    @Test
    fun analyzeStateHolder_ComplexModelWithMixedParams_ReturnsCorrectAnalysis() {
        // Arrange
        val model = StateHolderModel(
            className = COMPLEX_STATE_HOLDER,
            packageName = "com.example",
            constructorParams = listOf(
                ParameterModel("userId", "String", isInjected = true, injectionKey = "userId"),
                ParameterModel("scope", "CoroutineScope", isInjected = false, injectionKey = null),
                ParameterModel("config", "Config", isInjected = true, injectionKey = "appConfig"),
                ParameterModel("dispatcher", "CoroutineDispatcher", isInjected = false, injectionKey = null)
            ),
            sharedStates = listOf(
                SharedStateModel("userState", "MutableState<User>", isMutable = true),
                SharedStateModel("isLoading", "MutableState<Boolean>", isMutable = true)
            ),
            isInjectable = true
        )
        
        val expected = StateHolderAnalysis(
            moduleName = "ComplexModule",
            className = "ComplexStateHolder",
            factoryParams = listOf(
                ParameterModel("scope", "CoroutineScope", isInjected = false, injectionKey = null),
                ParameterModel("dispatcher", "CoroutineDispatcher", isInjected = false, injectionKey = null)
            ),
            shouldGenerate = true
        )
        
        // Act
        val result = analyzer.analyzeStateHolder(model)
        
        // Assert
        assertEquals(expected, result)
    }
    
    @Test
    fun analyzeStateHolder_SharedStatesPresence_DoesNotAffectCoreAnalysis() {
        // Arrange
        val modelWithSharedStates = createStateHolderModel(
            sharedStates = listOf(
                SharedStateModel("state1", "Type1", isMutable = true),
                SharedStateModel("state2", "Type2", isMutable = false)
            )
        )
        val modelWithoutSharedStates = createStateHolderModel(
            sharedStates = emptyList()
        )
        
        // Act
        val resultWithSharedStates = analyzer.analyzeStateHolder(modelWithSharedStates)
        val resultWithoutSharedStates = analyzer.analyzeStateHolder(modelWithoutSharedStates)
        
        // Assert - 両方の結果が同じ基本構造を持つことを確認
        assertEquals(resultWithSharedStates.moduleName, resultWithoutSharedStates.moduleName)
        assertEquals(resultWithSharedStates.className, resultWithoutSharedStates.className)
        assertEquals(resultWithSharedStates.factoryParams, resultWithoutSharedStates.factoryParams)
        assertEquals(resultWithSharedStates.shouldGenerate, resultWithoutSharedStates.shouldGenerate)
    }
    
    // endregion
    
    // region Helper Functions
    
    private fun createStateHolderModel(
        className: String = DEFAULT_CLASS_NAME,
        packageName: String = DEFAULT_PACKAGE,
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
    // endregion
}