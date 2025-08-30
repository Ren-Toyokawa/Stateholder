package io.github.rentoyokawa.stateholder.ksp.core

import io.github.rentoyokawa.stateholder.ksp.domain.models.ParameterModel
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StateHolderAnalyzerParameterTest {
    private val analyzer = StateHolderAnalyzer()

    @Test
    fun findInjectableParameters_AllParametersInjected_ReturnsAll() {
        // Arrange
        val params = listOf(
            ParameterModel(
                name = "userRepository",
                typeName = "UserRepository",
                isInjected = true,
                injectionKey = "userRepo"
            ),
            ParameterModel(
                name = "authService",
                typeName = "AuthService",
                isInjected = true,
                injectionKey = "authService"
            ),
            ParameterModel(
                name = "logger",
                typeName = "Logger",
                isInjected = true,
                injectionKey = "logger"
            )
        )

        // Act
        val result = analyzer.findInjectableParameters(params)

        // Assert
        assertEquals(3, result.size)
        assertEquals(params, result)
        assertTrue(result.all { it.isInjected })
    }

    @Test
    fun findInjectableParameters_PartiallyInjected_ReturnsOnlyInjected() {
        // Arrange
        val injectedParam1 = ParameterModel(
            name = "database",
            typeName = "Database",
            isInjected = true,
            injectionKey = "db"
        )
        val nonInjectedParam1 = ParameterModel(
            name = "scope",
            typeName = "CoroutineScope",
            isInjected = false,
            injectionKey = null
        )
        val injectedParam2 = ParameterModel(
            name = "apiClient",
            typeName = "ApiClient",
            isInjected = true,
            injectionKey = "api"
        )
        val nonInjectedParam2 = ParameterModel(
            name = "dispatcher",
            typeName = "CoroutineDispatcher",
            isInjected = false,
            injectionKey = null
        )

        val params = listOf(
            injectedParam1,
            nonInjectedParam1,
            injectedParam2,
            nonInjectedParam2
        )

        // Act
        val result = analyzer.findInjectableParameters(params)

        // Assert
        assertEquals(2, result.size)
        assertEquals(listOf(injectedParam1, injectedParam2), result)
        assertTrue(result.all { it.isInjected })
    }

    @Test
    fun findInjectableParameters_NoParametersInjected_ReturnsEmpty() {
        // Arrange
        val params = listOf(
            ParameterModel(
                name = "viewModelScope",
                typeName = "CoroutineScope",
                isInjected = false,
                injectionKey = null
            ),
            ParameterModel(
                name = "savedStateHandle",
                typeName = "SavedStateHandle",
                isInjected = false,
                injectionKey = null
            ),
            ParameterModel(
                name = "initialState",
                typeName = "State",
                isInjected = false,
                injectionKey = null
            )
        )

        // Act
        val result = analyzer.findInjectableParameters(params)

        // Assert
        assertTrue(result.isEmpty())
    }

    @Test
    fun findInjectableParameters_EmptyList_ReturnsEmpty() {
        // Arrange
        val params = emptyList<ParameterModel>()

        // Act
        val result = analyzer.findInjectableParameters(params)

        // Assert
        assertTrue(result.isEmpty())
    }

    @Test
    fun findInjectableParameters_PreservesOriginalOrder() {
        // Arrange
        val params = listOf(
            ParameterModel("param1", "Type1", isInjected = true, injectionKey = "key1"),
            ParameterModel("param2", "Type2", isInjected = false, injectionKey = null),
            ParameterModel("param3", "Type3", isInjected = true, injectionKey = "key3"),
            ParameterModel("param4", "Type4", isInjected = false, injectionKey = null),
            ParameterModel("param5", "Type5", isInjected = true, injectionKey = "key5")
        )

        // Act
        val result = analyzer.findInjectableParameters(params)

        // Assert
        assertEquals(3, result.size)
        assertEquals("param1", result[0].name)
        assertEquals("param3", result[1].name)
        assertEquals("param5", result[2].name)
    }
}