package test

import io.github.rentoyokawa.stateholder.StateHolder
import io.github.rentoyokawa.stateholder.annotations.InjectedParam
import io.github.rentoyokawa.stateholder.annotations.StateHolder as StateHolderAnnotation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

// Source data (internal state)
data class UserSource(
    val userState: UserSharedState,
    val isLoading: Boolean
)

// UI state (external state)
data class UserState(
    val username: String,
    val userId: String,
    val isLoggedIn: Boolean,
    val isLoading: Boolean
)

// Actions interface
interface UserAction {
    fun login(userId: String, username: String)
    fun logout()
    fun refresh()
}

@StateHolderAnnotation
class UserStateHolder(
    @InjectedParam("userState") private val userState: UserSharedState,
    scope: CoroutineScope
) : StateHolder<UserSource, UserState, UserAction>(scope) {
    
    private val _isLoading = MutableStateFlow(false)
    
    override fun defineState(source: UserSource): UserState {
        return UserState(
            username = source.userState.userName,
            userId = source.userState.userId,
            isLoggedIn = source.userState.userId.isNotEmpty(),
            isLoading = source.isLoading
        )
    }
    
    override fun createStateFlow(): Flow<UserSource> {
        return _isLoading.map { isLoading ->
            UserSource(userState, isLoading)
        }
    }
    
    override fun createInitialState(): UserState {
        return UserState(
            username = "",
            userId = "",
            isLoggedIn = false,
            isLoading = false
        )
    }
    
    override val action = object : UserAction {
        override fun login(userId: String, username: String) {
            _isLoading.value = true
            userState.userId = userId
            userState.userName = username
            _isLoading.value = false
        }
        
        override fun logout() {
            userState.userId = ""
            userState.userName = ""
        }
        
        override fun refresh() {
            _isLoading.value = true
            // Simulate refresh
            _isLoading.value = false
        }
    }
}