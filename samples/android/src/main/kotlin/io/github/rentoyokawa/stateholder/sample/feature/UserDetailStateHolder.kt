package io.github.rentoyokawa.stateholder.sample.feature

import io.github.rentoyokawa.stateholder.annotations.StateHolder as StateHolderAnnotation
import io.github.rentoyokawa.stateholder.core.SharedState
import io.github.rentoyokawa.stateholder.core.StateHolder
import io.github.rentoyokawa.stateholder.core.Store
import io.github.rentoyokawa.stateholder.sample.data.User
import io.github.rentoyokawa.stateholder.sample.data.UserRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf

/**
 * 詳細側の StateHolder。読み側の [UserDetailStore] を保持し、
 * 書き側の [UserDetailAction] を実装して合成する。
 *
 * `@StateHolder` を付与し、KSP（stateholder-processor-koin）が
 * [UserScreenViewModel] の Koin `factory { }` 定義を生成する対象にする（STA-14 成果の消費）。
 */
@StateHolderAnnotation
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

// ── Store: 選択 id を購読して詳細を combine 合成（読み側）─────

@OptIn(ExperimentalCoroutinesApi::class)
class UserDetailStore(
    private val selectedUserId: SharedState<String?>,
    private val repository: UserRepository,
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

    override fun defineState(source: UserDetailSource) = toUserDetailState(source)
}

// ── Local（書ける真実）／ Source ／ 純変換 ─────────────────

data class UserDetailLocal(val isFavorite: Boolean = false)

data class UserDetailSource(
    val local: UserDetailLocal,
    val user: User?,
)

fun toUserDetailState(source: UserDetailSource): UserDetailState =
    when (val user = source.user) {
        null -> UserDetailState.NothingSelected
        else -> UserDetailState.Loaded(user.name, user.bio, source.local.isFavorite)
    }
