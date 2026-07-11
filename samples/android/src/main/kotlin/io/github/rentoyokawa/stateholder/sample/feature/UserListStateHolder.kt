package io.github.rentoyokawa.stateholder.sample.feature

import io.github.rentoyokawa.stateholder.core.SharedState
import io.github.rentoyokawa.stateholder.core.StateHolder
import io.github.rentoyokawa.stateholder.core.Store
import io.github.rentoyokawa.stateholder.core.input
import io.github.rentoyokawa.stateholder.sample.data.User
import io.github.rentoyokawa.stateholder.sample.data.UserRepository
import kotlinx.coroutines.CoroutineScope

// ── Source: この holder の内部表現（外には漏れない）──────────

data class UserListSource(
    val query: String = "",
    val users: List<User> = emptyList(),
    val selectedId: String? = null,
)

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

/** Source → State の純変換。holder を組み立てずに直接テストできる */
fun toUserListState(source: UserListSource): UserListState = UserListState(
    query = source.query,
    rows = source.users
        .filter { it.name.contains(source.query, ignoreCase = true) }
        .map { UserListState.Row(it.id, it.name, isSelected = it.id == source.selectedId) },
)

/**
 * 一覧側の StateHolder（= ViewModel の一片）。
 * - 局所状態（query）は Store が保持する Source に置き、Action から update で編集
 * - 選択 id は [SharedState] へ書き、詳細側 holder と協調する
 */
class UserListStateHolder(
    private val selectedUserId: SharedState<String?>,
    repository: UserRepository,
    scope: CoroutineScope,
) : StateHolder<UserListState, UserListAction> {

    private val store = Store(
        initial = UserListSource(),
        scope = scope,
        inputs = listOf(
            input(repository.users) { s, users -> s.copy(users = users) },
            input(selectedUserId.flow) { s, id -> s.copy(selectedId = id) },
        ),
        defineState = ::toUserListState,
    )

    override val state = store.state

    override val action = object : UserListAction {
        override fun search(query: String) = store.update { it.copy(query = query) }
        override fun select(id: String) = selectedUserId.set(id)
    }
}
