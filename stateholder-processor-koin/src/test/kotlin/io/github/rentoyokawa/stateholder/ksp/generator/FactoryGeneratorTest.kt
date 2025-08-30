package io.github.rentoyokawa.stateholder.ksp.generator

import io.github.rentoyokawa.stateholder.ksp.core.StateHolderAnalysis
import io.github.rentoyokawa.stateholder.ksp.domain.models.ParameterModel
import org.junit.Test
import kotlin.test.assertEquals

class FactoryGeneratorTest {
    
    private val generator = KoinModuleGenerator()
    
    @Test
    fun `generateFactory with no parameters`() {
        // Arrange
        val analysis = StateHolderAnalysis(
            moduleName = "TestModule",
            className = "io.example.TestStateHolder",
            factoryParams = emptyList(),
            shouldGenerate = true
        )
        
        // Act
        val result = generator.generateFactoryCode(analysis)
        
        // Assert
        val expected = """
            factory<io.example.TestStateHolder> { ->
                io.example.TestStateHolder()
            }
        """.trimIndent()
        
        assertEquals(expected, result)
    }
    
    @Test
    fun `generateFactory with single parameter`() {
        // Arrange
        val analysis = StateHolderAnalysis(
            moduleName = "UserModule",
            className = "io.example.UserStateHolder",
            factoryParams = listOf(
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
        val result = generator.generateFactoryCode(analysis)
        
        // Assert
        val expected = """
            factory<io.example.UserStateHolder> { (scope: CoroutineScope) ->
                io.example.UserStateHolder(scope)
            }
        """.trimIndent()
        
        assertEquals(expected, result)
    }
    
    @Test
    fun `generateFactory with multiple parameters`() {
        // Arrange
        val analysis = StateHolderAnalysis(
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
                ),
                ParameterModel(
                    name = "logger",
                    typeName = "Logger",
                    isInjected = false,
                    injectionKey = null
                )
            ),
            shouldGenerate = true
        )
        
        // Act
        val result = generator.generateFactoryCode(analysis)
        
        // Assert
        val expected = """
            factory<io.example.DashboardStateHolder> { (scope: CoroutineScope, dispatcher: CoroutineDispatcher, logger: Logger) ->
                io.example.DashboardStateHolder(scope, dispatcher, logger)
            }
        """.trimIndent()
        
        assertEquals(expected, result)
    }
    
    @Test
    fun `generateFactory with special characters in names`() {
        // Arrange
        val analysis = StateHolderAnalysis(
            moduleName = "SpecialModule",
            className = "io.example.Special_StateHolder\$Inner",
            factoryParams = listOf(
                ParameterModel(
                    name = "_privateScope",
                    typeName = "CoroutineScope",
                    isInjected = false,
                    injectionKey = null
                ),
                ParameterModel(
                    name = "context\$app",
                    typeName = "ApplicationContext",
                    isInjected = false,
                    injectionKey = null
                )
            ),
            shouldGenerate = true
        )
        
        // Act
        val result = generator.generateFactoryCode(analysis)
        
        // Assert
        val expected = """
            factory<io.example.Special_StateHolder${'$'}Inner> { (_privateScope: CoroutineScope, context${'$'}app: ApplicationContext) ->
                io.example.Special_StateHolder${'$'}Inner(_privateScope, context${'$'}app)
            }
        """.trimIndent()
        
        assertEquals(expected, result)
    }
    
    @Test
    fun `generateFactory with generic type parameters`() {
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
                    name = "mapper",
                    typeName = "Function1<String, Item>",
                    isInjected = false,
                    injectionKey = null
                )
            ),
            shouldGenerate = true
        )
        
        // Act
        val result = generator.generateFactoryCode(analysis)
        
        // Assert
        val expected = """
            factory<io.example.GenericStateHolder> { (items: List<Item>, mapper: Function1<String, Item>) ->
                io.example.GenericStateHolder(items, mapper)
            }
        """.trimIndent()
        
        assertEquals(expected, result)
    }
    
    @Test
    fun `generateFactory with nullable type parameters`() {
        // Arrange
        val analysis = StateHolderAnalysis(
            moduleName = "NullableModule",
            className = "io.example.NullableStateHolder",
            factoryParams = listOf(
                ParameterModel(
                    name = "logger",
                    typeName = "Logger?",
                    isInjected = false,
                    injectionKey = null
                ),
                ParameterModel(
                    name = "config",
                    typeName = "Config?",
                    isInjected = false,
                    injectionKey = null
                )
            ),
            shouldGenerate = true
        )
        
        // Act
        val result = generator.generateFactoryCode(analysis)
        
        // Assert
        val expected = """
            factory<io.example.NullableStateHolder> { (logger: Logger?, config: Config?) ->
                io.example.NullableStateHolder(logger, config)
            }
        """.trimIndent()
        
        assertEquals(expected, result)
    }
    
    @Test
    fun `generateFactory with suspend lambda type parameters`() {
        // Arrange
        val analysis = StateHolderAnalysis(
            moduleName = "SuspendModule",
            className = "io.example.SuspendStateHolder",
            factoryParams = listOf(
                ParameterModel(
                    name = "onLoad",
                    typeName = "suspend () -> Unit",
                    isInjected = false,
                    injectionKey = null
                ),
                ParameterModel(
                    name = "onSave",
                    typeName = "suspend (String) -> Boolean",
                    isInjected = false,
                    injectionKey = null
                )
            ),
            shouldGenerate = true
        )
        
        // Act
        val result = generator.generateFactoryCode(analysis)
        
        // Assert
        val expected = """
            factory<io.example.SuspendStateHolder> { (onLoad: suspend () -> Unit, onSave: suspend (String) -> Boolean) ->
                io.example.SuspendStateHolder(onLoad, onSave)
            }
        """.trimIndent()
        
        assertEquals(expected, result)
    }
    
    @Test
    fun `generateFactory with lambda type parameters`() {
        // Arrange
        val analysis = StateHolderAnalysis(
            moduleName = "LambdaModule",
            className = "io.example.LambdaStateHolder",
            factoryParams = listOf(
                ParameterModel(
                    name = "onClick",
                    typeName = "() -> Unit",
                    isInjected = false,
                    injectionKey = null
                ),
                ParameterModel(
                    name = "onError",
                    typeName = "(String, Throwable) -> Unit",
                    isInjected = false,
                    injectionKey = null
                )
            ),
            shouldGenerate = true
        )
        
        // Act
        val result = generator.generateFactoryCode(analysis)
        
        // Assert
        val expected = """
            factory<io.example.LambdaStateHolder> { (onClick: () -> Unit, onError: (String, Throwable) -> Unit) ->
                io.example.LambdaStateHolder(onClick, onError)
            }
        """.trimIndent()
        
        assertEquals(expected, result)
    }
    
    @Test
    fun `generateFactory with nested generic type parameters`() {
        // Arrange
        val analysis = StateHolderAnalysis(
            moduleName = "NestedGenericModule",
            className = "io.example.NestedGenericStateHolder",
            factoryParams = listOf(
                ParameterModel(
                    name = "dataMap",
                    typeName = "Map<String, List<Item>>",
                    isInjected = false,
                    injectionKey = null
                ),
                ParameterModel(
                    name = "processor",
                    typeName = "Processor<Result<String>>",
                    isInjected = false,
                    injectionKey = null
                )
            ),
            shouldGenerate = true
        )
        
        // Act
        val result = generator.generateFactoryCode(analysis)
        
        // Assert
        val expected = """
            factory<io.example.NestedGenericStateHolder> { (dataMap: Map<String, List<Item>>, processor: Processor<Result<String>>) ->
                io.example.NestedGenericStateHolder(dataMap, processor)
            }
        """.trimIndent()
        
        assertEquals(expected, result)
    }
}