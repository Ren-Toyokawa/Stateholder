package io.github.rentoyokawa.stateholder.sample.feature

import io.github.rentoyokawa.stateholder.core.SharedState
import io.github.rentoyokawa.stateholder.core.StateHolder
import io.github.rentoyokawa.stateholder.core.Store
import io.github.rentoyokawa.stateholder.core.input
import io.github.rentoyokawa.stateholder.sample.data.User
import io.github.rentoyokawa.stateholder.sample.data.UserRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow

/**
 * 一覧側の StateHolder（= ViewModel の一片）。
 * 読み側の [UserListStore] を保持し、書き側の [UserListAction] を実装して合成する。
 */
class UserListStateHolder(
    private val selectedUserId: SharedState<String?>,
    repository: UserRepository,
    scope: CoroutineScope,
) : StateHolder<UserListState, UserListAction> {

    private val store = UserListStore(selectedUserId, repository, scope)

    override val state = store.state

    override val action = object : UserListAction {
        override fun search(query: String) = store.update { it.copy(query = query) }
        override fun select(id: String) = selectedUserId.set(id)
    }
}

// ── State / Action: View への読み書き契約 ─────────────────

data class UserListState(
    val query: String = "",
    val rows: List<Row> = emptyList(),
) {
    data class Row(val id: String, val name: String, val isSelected: Boolean)
}

interface UserListAction {
    fun search(query: String)
    fun select(id: String)
}

// ── Store: Source の保持・編集・State 導出（読み側）──────────

/**
 * 一覧の読み側。局所状態（query）と外部入力（users / 選択 id）を Source に束ね、
 * [toUserListState] で State を導出する。
 */
class UserListStore(
    selectedUserId: SharedState<String?>,
    repository: UserRepository,
    scope: CoroutineScope,
) : Store<UserListSource, UserListState>(scope) {

    override val initialSource = UserListSource()

    override val inputs: List<Flow<(UserListSource) -> UserListSource>> = listOf(
        input(repository.users) { s, users -> s.copy(users = users) },
        input(selectedUserId.flow) { s, id -> s.copy(selectedId = id) },
    )

    override fun defineState(source: UserListSource) = toUserListState(source)
}

// ── Source（内部表現）＋ 純変換 ───────────────────────────

data class UserListSource(
    val query: String = "",
    val users: List<User> = emptyList(),
    val selectedId: String? = null,
)

/** Source → State の純変換。holder を組み立てずに直接テストできる */
fun toUserListState(source: UserListSource): UserListState = UserListState(
    query = source.query,
    rows = source.users
        .filter { it.name.contains(source.query, ignoreCase = true) }
        .map { UserListState.Row(it.id, it.name, isSelected = it.id == source.selectedId) },
)
