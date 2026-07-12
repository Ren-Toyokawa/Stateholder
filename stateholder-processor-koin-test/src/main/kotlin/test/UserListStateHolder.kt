package test

import io.github.rentoyokawa.stateholder.annotations.StateHolder as StateHolderAnnotation
import io.github.rentoyokawa.stateholder.core.SharedState
import io.github.rentoyokawa.stateholder.core.StateHolder
import io.github.rentoyokawa.stateholder.core.Store
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/**
 * KSP 生成の統合テスト用一覧 holder。
 * `selectedUserId`（SharedState<String?>）を [UserDetailStateHolder] と共有する
 * （生成された Koin factory で同一インスタンスが結線されることが本テストの核）。
 */
data class UserListLocal(val query: String = "")

data class UserListSource(
    val local: UserListLocal,
    val users: List<User>,
    val selectedId: String?,
)

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

private fun toListState(source: UserListSource): UserListState = UserListState(
    query = source.local.query,
    rows = source.users
        .filter { it.name.contains(source.local.query, ignoreCase = true) }
        .map { UserListState.Row(it.id, it.name, isSelected = it.id == source.selectedId) },
)

private class UserListStore(
    private val selectedUserId: SharedState<String?>,
    private val repository: FakeUserRepository,
    scope: CoroutineScope,
) : Store<UserListLocal, UserListSource, UserListState>(scope) {
    override val initialLocal = UserListLocal()
    override val initialState = UserListState()
    override fun sources(local: Flow<UserListLocal>): Flow<UserListSource> =
        combine(local, repository.users, selectedUserId.flow, ::UserListSource)
    override fun defineState(source: UserListSource) = toListState(source)
}

@StateHolderAnnotation
class UserListStateHolder(
    private val selectedUserId: SharedState<String?>,
    repository: FakeUserRepository,
    scope: CoroutineScope,
) : StateHolder<UserListState, UserListAction> {

    private val store = UserListStore(selectedUserId, repository, scope)

    override val state = store.state

    override val action = object : UserListAction {
        override fun search(query: String) = store.update { it.copy(query = query) }
        override fun select(id: String) = selectedUserId.set(id)
    }
}
