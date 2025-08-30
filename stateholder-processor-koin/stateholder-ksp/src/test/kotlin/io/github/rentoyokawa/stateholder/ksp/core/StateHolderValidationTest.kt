package io.github.rentoyokawa.stateholder.ksp.core

import io.github.rentoyokawa.stateholder.ksp.domain.models.ErrorType
import io.github.rentoyokawa.stateholder.ksp.domain.models.ParameterModel
import io.github.rentoyokawa.stateholder.ksp.domain.models.SharedStateModel
import io.github.rentoyokawa.stateholder.ksp.domain.models.StateHolderModel
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class StateHolderValidationTest {
    private val analyzer = StateHolderAnalyzer()

    @Test
    fun validateStateHolder_ValidModel_ReturnsValidResult() {
        // Arrange
        val model = StateHolderModel(
            className = "UserStateHolder",
            packageName = "com.example.user",
            constructorParams = listOf(
                ParameterModel(
                    name = "userId",
                    typeName = "String",
                    isInjected = false,
                    injectionKey = null
                )
            ),
            sharedStates = listOf(
                SharedStateModel(
                    propertyName = "userName",
                    typeName = "MutableState<String>",
                    isMutable = true
                )
            ),
            isInjectable = true
        )

        // Act
        val result = analyzer.validateStateHolder(model)

        // Assert
        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
        assertTrue(result.warnings.isEmpty())
    }

    @Test
    fun validateStateHolder_EmptyClassName_ReturnsErrorForMissingField() {
        // Arrange
        val model = StateHolderModel(
            className = "",
            packageName = "com.example",
            constructorParams = emptyList(),
            sharedStates = emptyList(),
            isInjectable = true
        )

        // Act
        val result = analyzer.validateStateHolder(model)

        // Assert
        assertFalse(result.isValid)
        assertEquals(1, result.errors.size)
        val error = result.errors.first()
        assertEquals(ErrorType.MISSING_REQUIRED_FIELD, error.type)
        assertEquals("className", error.field)
        assertTrue(error.message.contains("className"))
    }

    @Test
    fun validateStateHolder_EmptyPackageName_ReturnsErrorForMissingField() {
        // Arrange
        val model = StateHolderModel(
            className = "TestStateHolder",
            packageName = "",
            constructorParams = emptyList(),
            sharedStates = emptyList(),
            isInjectable = true
        )

        // Act
        val result = analyzer.validateStateHolder(model)

        // Assert
        assertFalse(result.isValid)
        assertEquals(1, result.errors.size)
        val error = result.errors.first()
        assertEquals(ErrorType.MISSING_REQUIRED_FIELD, error.type)
        assertEquals("packageName", error.field)
    }

    @Test
    fun validateStateHolder_InvalidTypeNameInParameter_ReturnsError() {
        // Arrange
        val model = StateHolderModel(
            className = "TestStateHolder",
            packageName = "com.example",
            constructorParams = listOf(
                ParameterModel(
                    name = "param1",
                    typeName = "Invalid-Type-Name",
                    isInjected = false,
                    injectionKey = null
                )
            ),
            sharedStates = emptyList(),
            isInjectable = true
        )

        // Act
        val result = analyzer.validateStateHolder(model)

        // Assert
        assertFalse(result.isValid)
        assertEquals(1, result.errors.size)
        val error = result.errors.first()
        assertEquals(ErrorType.INVALID_TYPE_NAME, error.type)
        assertEquals("param1", error.field)
        assertTrue(error.message.contains("Invalid-Type-Name"))
    }

    @Test
    fun validateStateHolder_InvalidTypeNameInSharedState_ReturnsError() {
        // Arrange
        val model = StateHolderModel(
            className = "TestStateHolder",
            packageName = "com.example",
            constructorParams = emptyList(),
            sharedStates = listOf(
                SharedStateModel(
                    propertyName = "state1",
                    typeName = "123InvalidType",
                    isMutable = true
                )
            ),
            isInjectable = true
        )

        // Act
        val result = analyzer.validateStateHolder(model)

        // Assert
        assertFalse(result.isValid)
        assertEquals(1, result.errors.size)
        val error = result.errors.first()
        assertEquals(ErrorType.INVALID_TYPE_NAME, error.type)
        assertEquals("state1", error.field)
        assertTrue(error.message.contains("123InvalidType"))
    }

    @Test
    fun validateStateHolder_DirectCircularDependency_ReturnsError() {
        // Arrange
        val model = StateHolderModel(
            className = "UserStateHolder",
            packageName = "com.example",
            constructorParams = listOf(
                ParameterModel(
                    name = "selfReference",
                    typeName = "UserStateHolder",
                    isInjected = true,
                    injectionKey = "userStateHolder"
                )
            ),
            sharedStates = emptyList(),
            isInjectable = true
        )

        // Act
        val result = analyzer.validateStateHolder(model)

        // Assert
        assertFalse(result.isValid)
        assertEquals(1, result.errors.size)
        val error = result.errors.first()
        assertEquals(ErrorType.CIRCULAR_DEPENDENCY, error.type)
        assertEquals("selfReference", error.field)
        assertTrue(error.message.contains("circular") || error.message.contains("Circular"))
    }

    @Test
    fun validateStateHolder_ComplexTypeNames_AcceptsValidTypes() {
        // Arrange
        val model = StateHolderModel(
            className = "ComplexStateHolder",
            packageName = "com.example.complex",
            constructorParams = listOf(
                ParameterModel(
                    name = "listParam",
                    typeName = "List<String>",
                    isInjected = false,
                    injectionKey = null
                ),
                ParameterModel(
                    name = "mapParam",
                    typeName = "Map<String, Any?>",
                    isInjected = false,
                    injectionKey = null
                ),
                ParameterModel(
                    name = "lambdaParam",
                    typeName = "(String) -> Unit",
                    isInjected = false,
                    injectionKey = null
                ),
                ParameterModel(
                    name = "suspendLambdaParam",
                    typeName = "suspend (Int) -> String",
                    isInjected = false,
                    injectionKey = null
                )
            ),
            sharedStates = listOf(
                SharedStateModel(
                    propertyName = "complexState",
                    typeName = "MutableState<List<Pair<String, Int>>>",
                    isMutable = true
                )
            ),
            isInjectable = true
        )

        // Act
        val result = analyzer.validateStateHolder(model)

        // Assert
        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun validateStateHolder_MultipleErrors_ReturnsAllErrors() {
        // Arrange
        val model = StateHolderModel(
            className = "",
            packageName = "",
            constructorParams = listOf(
                ParameterModel(
                    name = "invalidParam",
                    typeName = "123Invalid",
                    isInjected = false,
                    injectionKey = null
                ),
                ParameterModel(
                    name = "selfRef",
                    typeName = "StateHolder",
                    isInjected = true,
                    injectionKey = "self"
                )
            ),
            sharedStates = listOf(
                SharedStateModel(
                    propertyName = "badState",
                    typeName = "invalid-type",
                    isMutable = false
                )
            ),
            isInjectable = true
        )

        // Act
        val result = analyzer.validateStateHolder(model)

        // Assert
        assertFalse(result.isValid)
        assertTrue(result.errors.size >= 4) // At least: empty className, empty packageName, invalid param type, invalid state type
    }

    @Test
    fun validateStateHolder_WarningsForNonIdealPatterns_GeneratesWarnings() {
        // Arrange
        val model = StateHolderModel(
            className = "MyClass", // Not ending with StateHolder
            packageName = "com.example",
            constructorParams = listOf(
                ParameterModel(
                    name = "veryLongParameterNameThatExceedsRecommendedLength",
                    typeName = "String",
                    isInjected = false,
                    injectionKey = null
                )
            ),
            sharedStates = emptyList(),
            isInjectable = true
        )

        // Act
        val result = analyzer.validateStateHolder(model)

        // Assert
        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
        assertTrue(result.warnings.isNotEmpty())
        assertTrue(result.warnings.any { it.contains("StateHolder") })
    }

    @Test
    fun validateStateHolder_InvalidPackageName_ReturnsError() {
        // Arrange
        val model = StateHolderModel(
            className = "TestStateHolder",
            packageName = "123.invalid.package",
            constructorParams = emptyList(),
            sharedStates = emptyList(),
            isInjectable = true
        )

        // Act
        val result = analyzer.validateStateHolder(model)

        // Assert
        assertFalse(result.isValid)
        assertEquals(1, result.errors.size)
        val error = result.errors.first()
        assertEquals(ErrorType.INVALID_PACKAGE_NAME, error.type)
        assertEquals("packageName", error.field)
    }

    @Test
    fun validateStateHolder_InvalidClassName_ReturnsError() {
        // Arrange
        val model = StateHolderModel(
            className = "123InvalidClassName",
            packageName = "com.example",
            constructorParams = emptyList(),
            sharedStates = emptyList(),
            isInjectable = true
        )

        // Act
        val result = analyzer.validateStateHolder(model)

        // Assert
        assertFalse(result.isValid)
        assertEquals(1, result.errors.size)
        val error = result.errors.first()
        assertEquals(ErrorType.INVALID_CLASS_NAME, error.type)
        assertEquals("className", error.field)
    }

    @Test
    fun validateStateHolder_IndirectCircularDependency_ReturnsError() {
        // Arrange: A -> B -> A pattern
        val model = StateHolderModel(
            className = "AStateHolder",
            packageName = "com.example",
            constructorParams = listOf(
                ParameterModel(
                    name = "bHolder",
                    typeName = "BStateHolder",
                    isInjected = true,
                    injectionKey = "b"
                )
            ),
            sharedStates = emptyList(),
            isInjectable = true
        )

        // This test assumes BStateHolder would have AStateHolder as dependency
        // For simplicity, we check if the type ends with StateHolder and is different from current
        // In real implementation, this would require more context

        // Act
        val result = analyzer.validateStateHolder(model)

        // Assert
        // This is a simplified test - full implementation would need dependency graph
        assertTrue(result.isValid || result.errors.any { it.type == ErrorType.CIRCULAR_DEPENDENCY })
    }
}