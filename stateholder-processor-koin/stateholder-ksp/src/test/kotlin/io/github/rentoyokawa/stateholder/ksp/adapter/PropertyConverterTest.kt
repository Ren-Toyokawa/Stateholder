package io.github.rentoyokawa.stateholder.ksp.adapter

import com.google.devtools.ksp.symbol.*
import io.github.rentoyokawa.stateholder.ksp.domain.models.SharedStateModel
import io.mockk.every
import io.mockk.mockk
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class PropertyConverterTest {
    private val converter = KSPModelConverter()
    
    @Test
    fun convertProperty_WithSharedStateAnnotation_ReturnsSharedStateModel() {
        // Arrange
        val property = createMockProperty(
            simpleName = "userName",
            typeName = "String",
            annotations = listOf(
                createMockAnnotation("SharedState")
            ),
            isMutable = true
        )
        
        // Act
        val result = converter.convertProperty(property)
        
        // Assert
        assertNotNull(result)
        assertEquals("userName", result.propertyName)
        assertEquals("String", result.typeName)
        assertTrue(result.isMutable)
    }
    
    @Test
    fun convertProperty_WithoutSharedStateAnnotation_ReturnsNull() {
        // Arrange
        val property = createMockProperty(
            simpleName = "normalProperty",
            typeName = "Int",
            annotations = emptyList(),
            isMutable = false
        )
        
        // Act
        val result = converter.convertProperty(property)
        
        // Assert
        assertNull(result)
    }
    
    @Test
    fun convertProperty_ImmutableSharedState_ReturnsMutableFalse() {
        // Arrange
        val property = createMockProperty(
            simpleName = "userId",
            typeName = "String",
            annotations = listOf(
                createMockAnnotation("SharedState")
            ),
            isMutable = false
        )
        
        // Act
        val result = converter.convertProperty(property)
        
        // Assert
        assertNotNull(result)
        assertEquals("userId", result.propertyName)
        assertEquals("String", result.typeName)
        assertFalse(result.isMutable)
    }
    
    @Test
    fun convertProperty_MutableSharedState_ReturnsMutableTrue() {
        // Arrange
        val property = createMockProperty(
            simpleName = "isLoading",
            typeName = "Boolean",
            annotations = listOf(
                createMockAnnotation("SharedState")
            ),
            isMutable = true
        )
        
        // Act
        val result = converter.convertProperty(property)
        
        // Assert
        assertNotNull(result)
        assertEquals("isLoading", result.propertyName)
        assertEquals("Boolean", result.typeName)
        assertTrue(result.isMutable)
    }
    
    @Test
    fun convertProperty_ComplexTypeWithSharedState_ReturnsCorrectTypeName() {
        // Arrange
        val property = createMockProperty(
            simpleName = "userList",
            typeName = "List<User>",
            typeDeclarationSimpleName = "List",
            annotations = listOf(
                createMockAnnotation("SharedState")
            ),
            isMutable = true
        )
        
        // Act
        val result = converter.convertProperty(property)
        
        // Assert
        assertNotNull(result)
        assertEquals("userList", result.propertyName)
        assertEquals("List", result.typeName) // 仕様通りdeclaration.simpleName.asString()を使用
        assertTrue(result.isMutable)
    }
    
    @Test
    fun convertProperty_WithOtherAnnotations_OnlyChecksSharedState() {
        // Arrange
        val property = createMockProperty(
            simpleName = "annotatedProperty",
            typeName = "String",
            annotations = listOf(
                createMockAnnotation("Serializable"),
                createMockAnnotation("SharedState"),
                createMockAnnotation("Deprecated")
            ),
            isMutable = false
        )
        
        // Act
        val result = converter.convertProperty(property)
        
        // Assert
        assertNotNull(result)
        assertEquals("annotatedProperty", result.propertyName)
        assertEquals("String", result.typeName)
        assertFalse(result.isMutable)
    }
    
    // Helper functions for creating mocks
    private fun createMockProperty(
        simpleName: String,
        typeName: String,
        typeDeclarationSimpleName: String = typeName,
        annotations: List<KSAnnotation>,
        isMutable: Boolean
    ): KSPropertyDeclaration {
        val mockProperty = mockk<KSPropertyDeclaration>()
        val mockName = mockk<KSName>()
        val mockType = mockk<KSTypeReference>()
        val mockResolvedType = mockk<KSType>()
        val mockDeclaration = mockk<KSDeclaration>()
        val mockDeclarationName = mockk<KSName>()
        
        every { mockName.asString() } returns simpleName
        every { mockDeclarationName.asString() } returns typeDeclarationSimpleName
        every { mockResolvedType.toString() } returns typeName
        every { mockResolvedType.declaration } returns mockDeclaration
        every { mockDeclaration.simpleName } returns mockDeclarationName
        every { mockType.resolve() } returns mockResolvedType
        every { mockProperty.simpleName } returns mockName
        every { mockProperty.type } returns mockType
        every { mockProperty.annotations } returns annotations.asSequence()
        every { mockProperty.isMutable } returns isMutable
        
        return mockProperty
    }
    
    private fun createMockAnnotation(shortName: String): KSAnnotation {
        val mockAnnotation = mockk<KSAnnotation>()
        val mockShortName = mockk<KSName>()
        
        every { mockShortName.asString() } returns shortName
        every { mockAnnotation.shortName } returns mockShortName
        every { mockAnnotation.arguments } returns emptyList()
        
        return mockAnnotation
    }
}