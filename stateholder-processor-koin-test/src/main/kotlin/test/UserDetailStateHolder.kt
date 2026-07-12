package test

import io.github.rentoyokawa.stateholder.annotations.StateHolder as StateHolderAnnotation
import io.github.rentoyokawa.stateholder.core.SharedState
import io.github.rentoyokawa.stateholder.core.StateHolder
import io.github.rentoyokawa.stateholder.core.Store
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf

/**
 * KSP 生成の統合テスト用詳細 holder。
 * `selectedUserId`（SharedState<String?>）を [UserListStateHolder] と共有する。
 */
data class UserDetailLocal(val isFavorite: Boolean = false)

data class UserDetailSource(
    val local: UserDetailLocal,
    val user: User?,
)

sealed interface UserDetailState {
    data object NothingSelected : UserDetailState
    data class Loaded(val name: String, val isFavorite: Boolean) : UserDetailState
}

interface UserDetailAction {
    fun toggleFavorite()
}

private fun toDetailState(source: UserDetailSource): UserDetailState =
    when (val user = source.user) {
        null -> UserDetailState.NothingSelected
        else -> UserDetailState.Loaded(user.name, source.local.isFavorite)
    }

@OptIn(ExperimentalCoroutinesApi::class)
private class UserDetailStore(
    private val selectedUserId: SharedState<String?>,
    private val repository: FakeUserRepository,
    scope: CoroutineScope,
) : Store<UserDetailLocal, UserDetailSource, UserDetailState>(scope) {
    override val initialLocal = UserDetailLocal()
    override val initialState = UserDetailState.NothingSelected
    override fun sources(local: Flow<UserDetailLocal>): Flow<UserDetailSource> =
        combine(
            local,
            selectedUserId.flow.flatMapLatest { id ->
                if (id == null) flowOf(null) else repository.user(id)
            },
            ::UserDetailSource,
        )
    override fun defineState(source: UserDetailSource) = toDetailState(source)
}

@StateHolderAnnotation
class UserDetailStateHolder(
    private val selectedUserId: SharedState<String?>,
    repository: FakeUserRepository,
    scope: CoroutineScope,
) : StateHolder<UserDetailState, UserDetailAction> {

    private val store = UserDetailStore(selectedUserId, repository, scope)

    override val state = store.state

    override val action = object : UserDetailAction {
        override fun toggleFavorite() = store.update { it.copy(isFavorite = !it.isFavorite) }
    }
}
