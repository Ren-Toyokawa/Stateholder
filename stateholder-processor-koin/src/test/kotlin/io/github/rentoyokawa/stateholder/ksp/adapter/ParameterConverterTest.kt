package io.github.rentoyokawa.stateholder.ksp.adapter

import com.google.devtools.ksp.symbol.*
import io.mockk.every
import io.mockk.mockk
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * KSPModelConverter.convertParameterのテスト
 * MockKを使用して単一パラメータの変換をテスト
 */
class ParameterConverterTest {
    
    private val converter = KSPModelConverter()
    
    @Test
    fun `convertParameter - パラメータのみの場合`() {
        // Arrange
        val mockParam = createMockParameter(
            name = "userId",
            typeName = "String",
            annotations = emptyList()
        )
        
        // Act
        val result = converter.convertParameter(mockParam)
        
        // Assert
        assertEquals("userId", result.name)
        assertEquals("String", result.typeName)
        assertFalse(result.isInjected)
        assertNull(result.injectionKey)
    }
    
    @Test
    fun `convertParameter - InjectedParam付きでkeyが指定されている場合`() {
        // Arrange
        val injectedAnnotation = createMockAnnotation("InjectedParam", mapOf("key" to "user_id"))
        val mockParam = createMockParameter(
            name = "userId",
            typeName = "String",
            annotations = listOf(injectedAnnotation)
        )
        
        // Act
        val result = converter.convertParameter(mockParam)
        
        // Assert
        assertEquals("userId", result.name)
        assertEquals("String", result.typeName)
        assertTrue(result.isInjected)
        assertEquals("user_id", result.injectionKey)
    }
    
    @Test
    fun `convertParameter - InjectedParam付きでkeyが指定されていない場合`() {
        // Arrange
        val injectedAnnotation = createMockAnnotation("InjectedParam", emptyMap())
        val mockParam = createMockParameter(
            name = "userName",
            typeName = "String",
            annotations = listOf(injectedAnnotation)
        )
        
        // Act
        val result = converter.convertParameter(mockParam)
        
        // Assert
        assertEquals("userName", result.name)
        assertEquals("String", result.typeName)
        assertTrue(result.isInjected)
        assertEquals("userName", result.injectionKey) // パラメータ名が使用される
    }
    
    @Test
    fun `convertParameter - 複数のアノテーションがある場合`() {
        // Arrange
        val otherAnnotation = createMockAnnotation("OtherAnnotation", emptyMap())
        val injectedAnnotation = createMockAnnotation("InjectedParam", mapOf("key" to "custom_key"))
        val mockParam = createMockParameter(
            name = "scope",
            typeName = "CoroutineScope",
            annotations = listOf(otherAnnotation, injectedAnnotation)
        )
        
        // Act
        val result = converter.convertParameter(mockParam)
        
        // Assert
        assertEquals("scope", result.name)
        assertEquals("CoroutineScope", result.typeName)
        assertTrue(result.isInjected)
        assertEquals("custom_key", result.injectionKey)
    }
    
    @Test
    fun `convertParameter - 異なる型のパラメータ`() {
        // Arrange
        val testCases = listOf(
            Triple("intParam", "Int", emptyList<KSAnnotation>()),
            Triple("boolParam", "Boolean", emptyList<KSAnnotation>()),
            Triple("listParam", "List<String>", emptyList<KSAnnotation>()),
            Triple("mapParam", "Map<String, Any>", emptyList<KSAnnotation>())
        )
        
        testCases.forEach { (name, type, annotations) ->
            // Arrange
            val mockParam = createMockParameter(
                name = name,
                typeName = type,
                annotations = annotations
            )
            
            // Act
            val result = converter.convertParameter(mockParam)
            
            // Assert
            assertEquals(name, result.name)
            assertEquals(type, result.typeName)
            assertFalse(result.isInjected)
            assertNull(result.injectionKey)
        }
    }
    
    @Test
    fun `convertParameter - keyパラメータが不正な型の場合`() {
        // Arrange
        val injectedAnnotation = createMockAnnotation("InjectedParam", mapOf("key" to 123)) // Stringではなく数値
        val mockParam = createMockParameter(
            name = "testParam",
            typeName = "String",
            annotations = listOf(injectedAnnotation)
        )
        
        // Act
        val result = converter.convertParameter(mockParam)
        
        // Assert
        assertEquals("testParam", result.name)
        assertEquals("String", result.typeName)
        assertTrue(result.isInjected)
        assertEquals("testParam", result.injectionKey) // パラメータ名にフォールバック
    }
    
    // Helper functions for creating mocks
    private fun createMockParameter(
        name: String,
        typeName: String,
        annotations: List<KSAnnotation>
    ): KSValueParameter {
        val mockParam = mockk<KSValueParameter>()
        val mockName = mockk<KSName>()
        val mockType = mockk<KSTypeReference>()
        val mockResolvedType = mockk<KSType>()
        
        every { mockName.asString() } returns name
        every { mockResolvedType.toString() } returns typeName
        every { mockType.resolve() } returns mockResolvedType
        every { mockParam.name } returns mockName
        every { mockParam.type } returns mockType
        every { mockParam.annotations } returns annotations.asSequence()
        
        return mockParam
    }
    
    private fun createMockAnnotation(shortName: String, arguments: Map<String, Any>): KSAnnotation {
        val mockAnnotation = mockk<KSAnnotation>()
        val mockShortName = mockk<KSName>()
        
        every { mockShortName.asString() } returns shortName
        every { mockAnnotation.shortName } returns mockShortName
        
        val mockArguments = arguments.map { (key, value) ->
            val mockArgument = mockk<KSValueArgument>()
            val mockArgumentName = mockk<KSName>()
            every { mockArgumentName.asString() } returns key
            every { mockArgument.name } returns mockArgumentName
            every { mockArgument.value } returns value
            mockArgument
        }
        
        every { mockAnnotation.arguments } returns mockArguments
        
        return mockAnnotation
    }
}