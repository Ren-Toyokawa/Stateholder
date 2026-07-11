package io.github.rentoyokawa.stateholder.sample.feature

import io.github.rentoyokawa.stateholder.core.SharedState
import io.github.rentoyokawa.stateholder.core.StateHolder
import io.github.rentoyokawa.stateholder.core.Store
import io.github.rentoyokawa.stateholder.core.input
import io.github.rentoyokawa.stateholder.sample.data.User
import io.github.rentoyokawa.stateholder.sample.data.UserRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf

/**
 * 詳細側の StateHolder。読み側の [UserDetailStore] を保持し、
 * 書き側の [UserDetailAction] を実装して合成する。
 */
class UserDetailStateHolder(
    selectedUserId: SharedState<String?>,
    repository: UserRepository,
    scope: CoroutineScope,
) : StateHolder<UserDetailState, UserDetailAction> {

    private val store = UserDetailStore(selectedUserId, repository, scope)

    override val state = store.state

    override val action = object : UserDetailAction {
        override fun toggleFavorite() = store.update { it.copy(isFavorite = !it.isFavorite) }
    }
}

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

// ── Store: 選択 id を購読して詳細を追従（読み側）─────────────

@OptIn(ExperimentalCoroutinesApi::class)
class UserDetailStore(
    selectedUserId: SharedState<String?>,
    repository: UserRepository,
    scope: CoroutineScope,
) : Store<UserDetailSource, UserDetailState>(scope) {

    override val initialSource = UserDetailSource()

    override val inputs: List<Flow<(UserDetailSource) -> UserDetailSource>> = listOf(
        input(
            selectedUserId.flow.flatMapLatest { id ->
                if (id == null) flowOf(null) else repository.user(id)
            },
        ) { s, user -> s.copy(user = user) },
    )

    override fun defineState(source: UserDetailSource) = toUserDetailState(source)
}

// ── Source（内部表現）＋ 純変換 ───────────────────────────

data class UserDetailSource(
    val user: User? = null,
    val isFavorite: Boolean = false,
)

fun toUserDetailState(source: UserDetailSource): UserDetailState =
    when (val user = source.user) {
        null -> UserDetailState.NothingSelected
        else -> UserDetailState.Loaded(user.name, user.bio, source.isFavorite)
    }
