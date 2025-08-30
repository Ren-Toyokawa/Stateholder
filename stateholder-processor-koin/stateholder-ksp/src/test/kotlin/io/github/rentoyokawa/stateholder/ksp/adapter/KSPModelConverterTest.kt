package io.github.rentoyokawa.stateholder.ksp.adapter

import com.google.devtools.ksp.symbol.*
import io.github.rentoyokawa.stateholder.ksp.domain.models.ParameterModel
import io.github.rentoyokawa.stateholder.ksp.domain.models.SharedStateModel
import io.github.rentoyokawa.stateholder.ksp.domain.models.StateHolderModel
import io.mockk.every
import io.mockk.mockk
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class KSPModelConverterTest {
    private val converter = KSPModelConverter()

    @Test
    fun convertClass_SimpleStateHolder_ReturnsCorrectModel() {
        // Arrange
        val mockClass = createMockKSClassDeclaration(
            simpleName = "UserStateHolder",
            packageName = "com.example.app",
            annotations = emptyList(),
            constructorParams = emptyList(),
            properties = emptyList()
        )

        // Act
        val result = converter.convertClass(mockClass)

        // Assert
        assertEquals("UserStateHolder", result.className)
        assertEquals("com.example.app", result.packageName)
        assertTrue(result.constructorParams.isEmpty())
        assertTrue(result.sharedStates.isEmpty())
        assertFalse(result.isInjectable)
    }

    @Test
    fun convertClass_WithInjectableAnnotation_ReturnsInjectableTrue() {
        // Arrange
        val mockAnnotation = createMockAnnotation("StateHolder", mapOf("injectable" to true))
        val mockClass = createMockKSClassDeclaration(
            simpleName = "UserStateHolder",
            packageName = "com.example.app",
            annotations = listOf(mockAnnotation),
            constructorParams = emptyList(),
            properties = emptyList()
        )

        // Act
        val result = converter.convertClass(mockClass)

        // Assert
        assertTrue(result.isInjectable)
    }

    @Test
    fun convertClass_WithConstructorParameters_ReturnsCorrectParams() {
        // Arrange
        val param1 = createMockParameter("userId", "String", emptyList())
        val param2 = createMockParameter("scope", "CoroutineScope", emptyList())
        
        val mockClass = createMockKSClassDeclaration(
            simpleName = "UserStateHolder",
            packageName = "com.example.app",
            annotations = emptyList(),
            constructorParams = listOf(param1, param2),
            properties = emptyList()
        )

        // Act
        val result = converter.convertClass(mockClass)

        // Assert
        assertEquals(2, result.constructorParams.size)
        assertEquals("userId", result.constructorParams[0].name)
        assertEquals("String", result.constructorParams[0].typeName)
        assertFalse(result.constructorParams[0].isInjected)
        assertEquals("scope", result.constructorParams[1].name)
        assertEquals("CoroutineScope", result.constructorParams[1].typeName)
        assertFalse(result.constructorParams[1].isInjected)
    }

    @Test
    fun convertClass_WithInjectedParam_ReturnsCorrectInjectionInfo() {
        // Arrange
        val injectedAnnotation1 = createMockAnnotation("InjectedParam", mapOf("key" to "userId"))
        val injectedAnnotation2 = createMockAnnotation("InjectedParam", emptyMap())
        
        val param1 = createMockParameter("userId", "String", listOf(injectedAnnotation1))
        val param2 = createMockParameter("userName", "String", listOf(injectedAnnotation2))
        
        val mockClass = createMockKSClassDeclaration(
            simpleName = "UserStateHolder",
            packageName = "com.example.app",
            annotations = emptyList(),
            constructorParams = listOf(param1, param2),
            properties = emptyList()
        )

        // Act
        val result = converter.convertClass(mockClass)

        // Assert
        assertEquals(2, result.constructorParams.size)
        assertTrue(result.constructorParams[0].isInjected)
        assertEquals("userId", result.constructorParams[0].injectionKey)
        assertTrue(result.constructorParams[1].isInjected)
        assertEquals("userName", result.constructorParams[1].injectionKey)
    }

    @Test
    fun convertClass_WithSharedStateProperties_ReturnsCorrectSharedStates() {
        // Arrange
        val sharedStateAnnotation = createMockAnnotation("SharedState", emptyMap())
        
        val prop1 = createMockProperty("userName", "MutableState<String>", listOf(sharedStateAnnotation), true)
        val prop2 = createMockProperty("userId", "String", listOf(sharedStateAnnotation), false)
        val prop3 = createMockProperty("nonSharedProperty", "Int", emptyList(), false)
        
        val mockClass = createMockKSClassDeclaration(
            simpleName = "UserStateHolder",
            packageName = "com.example.app",
            annotations = emptyList(),
            constructorParams = emptyList(),
            properties = listOf(prop1, prop2, prop3)
        )

        // Act
        val result = converter.convertClass(mockClass)

        // Assert
        assertEquals(2, result.sharedStates.size)
        assertEquals("userName", result.sharedStates[0].propertyName)
        assertEquals("MutableState<String>", result.sharedStates[0].typeName)
        assertTrue(result.sharedStates[0].isMutable)
        assertEquals("userId", result.sharedStates[1].propertyName)
        assertEquals("String", result.sharedStates[1].typeName)
        assertFalse(result.sharedStates[1].isMutable)
    }

    @Test
    fun convertClass_ComplexStateHolder_ReturnsCompleteModel() {
        // Arrange
        val stateHolderAnnotation = createMockAnnotation("StateHolder", mapOf("injectable" to true))
        val injectedParamAnnotation = createMockAnnotation("InjectedParam", mapOf("key" to "user_id"))
        val sharedStateAnnotation = createMockAnnotation("SharedState", emptyMap())
        
        val param1 = createMockParameter("userId", "String", listOf(injectedParamAnnotation))
        val param2 = createMockParameter("scope", "CoroutineScope", emptyList())
        
        val prop1 = createMockProperty("userState", "MutableState<User>", listOf(sharedStateAnnotation), true)
        val prop2 = createMockProperty("isLoading", "MutableState<Boolean>", listOf(sharedStateAnnotation), true)
        
        val mockClass = createMockKSClassDeclaration(
            simpleName = "ComplexStateHolder",
            packageName = "com.example.complex",
            annotations = listOf(stateHolderAnnotation),
            constructorParams = listOf(param1, param2),
            properties = listOf(prop1, prop2)
        )

        val expected = StateHolderModel(
            className = "ComplexStateHolder",
            packageName = "com.example.complex",
            constructorParams = listOf(
                ParameterModel(
                    name = "userId",
                    typeName = "String",
                    isInjected = true,
                    injectionKey = "user_id"
                ),
                ParameterModel(
                    name = "scope",
                    typeName = "CoroutineScope",
                    isInjected = false,
                    injectionKey = null
                )
            ),
            sharedStates = listOf(
                SharedStateModel(
                    propertyName = "userState",
                    typeName = "MutableState<User>",
                    isMutable = true
                ),
                SharedStateModel(
                    propertyName = "isLoading",
                    typeName = "MutableState<Boolean>",
                    isMutable = true
                )
            ),
            isInjectable = true
        )

        // Act
        val result = converter.convertClass(mockClass)

        // Assert
        assertEquals(expected, result)
    }

    // Helper functions for creating mocks
    private fun createMockKSClassDeclaration(
        simpleName: String,
        packageName: String,
        annotations: List<KSAnnotation>,
        constructorParams: List<KSValueParameter>,
        properties: List<KSPropertyDeclaration>
    ): KSClassDeclaration {
        val mockClass = mockk<KSClassDeclaration>()
        val mockSimpleName = mockk<KSName>()
        val mockPackageName = mockk<KSName>()
        
        every { mockSimpleName.asString() } returns simpleName
        every { mockPackageName.asString() } returns packageName
        every { mockClass.simpleName } returns mockSimpleName
        every { mockClass.packageName } returns mockPackageName
        every { mockClass.annotations } returns annotations.asSequence()
        every { mockClass.getAllProperties() } returns properties.asSequence()
        
        if (constructorParams.isNotEmpty()) {
            val mockConstructor = mockk<KSFunctionDeclaration>()
            every { mockConstructor.parameters } returns constructorParams
            every { mockClass.primaryConstructor } returns mockConstructor
        } else {
            every { mockClass.primaryConstructor } returns null
        }
        
        return mockClass
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

    private fun createMockProperty(
        simpleName: String,
        typeName: String,
        annotations: List<KSAnnotation>,
        isMutable: Boolean
    ): KSPropertyDeclaration {
        val mockProperty = mockk<KSPropertyDeclaration>()
        val mockName = mockk<KSName>()
        val mockType = mockk<KSTypeReference>()
        val mockResolvedType = mockk<KSType>()
        
        every { mockName.asString() } returns simpleName
        every { mockResolvedType.toString() } returns typeName
        every { mockType.resolve() } returns mockResolvedType
        every { mockProperty.simpleName } returns mockName
        every { mockProperty.type } returns mockType
        every { mockProperty.annotations } returns annotations.asSequence()
        every { mockProperty.isMutable } returns isMutable
        
        return mockProperty
    }
}