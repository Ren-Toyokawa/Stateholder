package io.github.rentoyokawa.stateholder.sample.feature

import io.github.rentoyokawa.stateholder.core.SharedState
import io.github.rentoyokawa.stateholder.core.StateHolder
import io.github.rentoyokawa.stateholder.core.Store
import io.github.rentoyokawa.stateholder.sample.data.User
import io.github.rentoyokawa.stateholder.sample.data.UserRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

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

// ── Store: Local の保持 ＋ 外部データの combine 合成（読み側）────

/**
 * 一覧の読み側。書ける局所状態（[UserListLocal]）を保持し、
 * 外部データ（users / 選択 id）を [sources] の combine で合成して [toUserListState] で導出する。
 */
class UserListStore(
    private val selectedUserId: SharedState<String?>,
    private val repository: UserRepository,
    scope: CoroutineScope,
) : Store<UserListLocal, UserListSource, UserListState>(scope) {

    override val initialLocal = UserListLocal()
    override val initialState = UserListState()

    override fun sources(local: Flow<UserListLocal>): Flow<UserListSource> =
        combine(local, repository.users, selectedUserId.flow, ::UserListSource)

    override fun defineState(source: UserListSource) = toUserListState(source)
}

// ── Local（書ける真実）／ Source（合成した内部表現）／ 純変換 ────

data class UserListLocal(val query: String = "")

data class UserListSource(
    val local: UserListLocal,
    val users: List<User>,
    val selectedId: String?,
)

/** Source → State の純変換。holder を組み立てずに直接テストできる */
fun toUserListState(source: UserListSource): UserListState = UserListState(
    query = source.local.query,
    rows = source.users
        .filter { it.name.contains(source.local.query, ignoreCase = true) }
        .map { UserListState.Row(it.id, it.name, isSelected = it.id == source.selectedId) },
)
