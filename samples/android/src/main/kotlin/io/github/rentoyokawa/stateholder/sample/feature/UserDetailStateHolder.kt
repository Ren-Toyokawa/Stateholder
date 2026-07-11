package io.github.rentoyokawa.stateholder.sample.feature

import io.github.rentoyokawa.stateholder.core.SharedState
import io.github.rentoyokawa.stateholder.core.StateHolder
import io.github.rentoyokawa.stateholder.core.Store
import io.github.rentoyokawa.stateholder.core.input
import io.github.rentoyokawa.stateholder.sample.data.User
import io.github.rentoyokawa.stateholder.sample.data.UserRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf

// ── Source ────────────────────────────────────────────

data class UserDetailSource(
    val user: User? = null,
    val isFavorite: Boolean = false,
)

// ── State / Action ────────────────────────────────────

sealed interface UserDetailState {
    data object NothingSelected : UserDetailState
    data class Loaded(
        val name: String,
        val bio: String,
        val isFavorite: Boolean,
    ) : UserDetailState
}

interface UserDetailAction {
    fun toggleFavorite()
}

fun toUserDetailState(source: UserDetailSource): UserDetailState =
    when (val user = source.user) {
        null -> UserDetailState.NothingSelected
        else -> UserDetailState.Loaded(user.name, user.bio, source.isFavorite)
    }

/**
 * 詳細側の StateHolder。
 * [SharedState] の選択 id を購読し、一覧側の select に追従する。
 */
@OptIn(ExperimentalCoroutinesApi::class)
class UserDetailStateHolder(
    selectedUserId: SharedState<String?>,
    repository: UserRepository,
    scope: CoroutineScope,
) : StateHolder<UserDetailState, UserDetailAction> {

    private val store = Store(
        initial = UserDetailSource(),
        scope = scope,
        inputs = listOf(
            input(
                selectedUserId.flow.flatMapLatest { id ->
                    if (id == null) flowOf(null) else repository.user(id)
                },
            ) { s, user -> s.copy(user = user) },
        ),
        defineState = ::toUserDetailState,
    )

    override val state = store.state

    override val action = object : UserDetailAction {
        override fun toggleFavorite() = store.update { it.copy(isFavorite = !it.isFavorite) }
    }
}
