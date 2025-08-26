package io.github.rentoyokawa.stateholder

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * StateHolder for managing UI state with pure functions
 * @param Source The type of the source data (internal state)
 * @param State The type of the UI state (external state)
 * @param Action The type of actions that can be performed
 */
abstract class StateHolder<Source, State, Action>(
    protected val scope: CoroutineScope
) {
    /**
     * Pure function that transforms internal state (Source) to UI state (State)
     * This function should have no side effects
     */
    abstract fun defineState(source: Source): State
    
    /**
     * Creates a Flow of Source by combining multiple data streams
     * This is where you combine repositories, shared states, and other data sources
     */
    abstract fun createStateFlow(): Flow<Source>
    
    /**
     * Defines the initial state of the UI
     * This is used before any data is loaded
     */
    abstract fun createInitialState(): State
    
    /**
     * The action interface implementation
     * This contains all user-triggered actions
     */
    abstract val action: Action
    
    /**
     * The final UI state exposed as StateFlow
     * Automatically computed from createStateFlow() and defineState()
     */
    val state: StateFlow<State> by lazy {
        createStateFlow()
            .map(::defineState)
            .stateIn(
                scope = scope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = createInitialState()
            )
    }
}