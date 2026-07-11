package io.github.rentoyokawa.stateholder.core

import app.cash.turbine.test
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

// ═══════════════════════════════════════════════════════════════════
// テストフィクスチャ = 新設計での「実際の書き方」の実例
// 各 feature は  専用 Store（読み）＋ Action（書き）＋ StateHolder（合成）  で構成する。
// 外部データは sources() の combine で合成、書けるのは Local だけ。
// （一覧でユーザーを選ぶと詳細が SharedState 経由で追従するシナリオ）
// ═══════════════════════════════════════════════════════════════════

private data class User(val id: String, val name: String)

private class FakeUserRepository {
    val users = MutableStateFlow<List<User>>(emptyList())
    fun user(id: String): Flow<User?> = users.map { list -> list.find { it.id == id } }
}

// ── 一覧側 ──────────────────────────────────────────────

private data class UserListLocal(val query: String = "")

private data class UserListSource(
    val local: UserListLocal,
    val users: List<User>,
    val selectedId: String?,
)

private data class UserListState(
    val query: String = "",
    val rows: List<Row> = emptyList(),
) {
    data class Row(val id: String, val name: String, val isSelected: Boolean)
}

private interface UserListAction {
    fun search(query: String)
    fun select(id: String)
}

// 純変換はトップレベル関数 → 単体で直接テスト可能
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

private class UserListStateHolder(
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

// ── 詳細側 ──────────────────────────────────────────────

private data class UserDetailLocal(val isFavorite: Boolean = false)

private data class UserDetailSource(
    val local: UserDetailLocal,
    val user: User?,
)

private sealed interface UserDetailState {
    data object NothingSelected : UserDetailState
    data class Loaded(val name: String, val isFavorite: Boolean) : UserDetailState
}

private interface UserDetailAction {
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

private class UserDetailStateHolder(
    selectedUserId: SharedState<String?>,
    repository: FakeUserRepository,
    scope: CoroutineScope,
) : StateHolder<UserDetailState, UserDetailAction> {

    private val store = UserDetailStore(selectedUserId, repository, scope)

    override val state = store.state

    override val action = object : UserDetailAction {
        override fun toggleFavorite() = store.update { it.copy(isFavorite = !it.isFavorite) }
    }
}

// ═══════════════════════════════════════════════════════════════════
// シナリオテスト
// ═══════════════════════════════════════════════════════════════════

class StateHolderScenarioTest {

    private val alice = User("u1", "Alice")
    private val bob = User("u2", "Bob")

    @Test
    fun `一覧のselectがSharedState経由で詳細holderに伝播する`() = runTest {
        val shared = SharedState<String?>(null)
        val repo = FakeUserRepository().apply { users.value = listOf(alice, bob) }
        val list = UserListStateHolder(shared, repo, backgroundScope)
        val detail = UserDetailStateHolder(shared, repo, backgroundScope)

        detail.state.test {
            assertEquals(UserDetailState.NothingSelected, awaitItem())

            list.action.select(alice.id)   // 一覧の Action が共有(id)を書く

            val loaded = assertIs<UserDetailState.Loaded>(awaitItem())
            assertEquals("Alice", loaded.name)
        }
    }

    @Test
    fun `selectは自分(一覧)のstateにも還流してisSelectedが立つ`() = runTest {
        val shared = SharedState<String?>(null)
        val repo = FakeUserRepository().apply { users.value = listOf(alice, bob) }
        val list = UserListStateHolder(shared, repo, backgroundScope)

        list.state.test {
            skipItems(1) // 初期値（users 合流前）
            assertEquals(2, awaitItem().rows.size)

            list.action.select(bob.id)     // 共有へ書く → sources 合成経由で自分にも還流

            val state = awaitItem()
            assertTrue(state.rows.single { it.id == bob.id }.isSelected)
            assertTrue(state.rows.single { it.id == alice.id }.isSelected.not())
        }
    }

    @Test
    fun `局所状態(query)の編集は他のholderに影響しない`() = runTest {
        val shared = SharedState<String?>(null)
        val repo = FakeUserRepository().apply { users.value = listOf(alice, bob) }
        val list = UserListStateHolder(shared, repo, backgroundScope)
        val detail = UserDetailStateHolder(shared, repo, backgroundScope)

        list.state.test {
            skipItems(2) // 初期値 + users 合流

            list.action.search("ali")      // 局所編集（Local を update）
            val filtered = awaitItem()
            assertEquals(listOf("Alice"), filtered.rows.map { it.name })
        }
        // 詳細側は無風のまま
        assertEquals(UserDetailState.NothingSelected, detail.state.value)
    }

    @Test
    fun `リポジトリの変化が選択中の詳細に追従する`() = runTest {
        val shared = SharedState<String?>(alice.id)
        val repo = FakeUserRepository().apply { users.value = listOf(alice, bob) }
        val detail = UserDetailStateHolder(shared, repo, backgroundScope)

        detail.state.test {
            skipItems(1) // NothingSelected（user 合流前）
            assertEquals(UserDetailState.Loaded("Alice", isFavorite = false), awaitItem())

            // 上流データが変わると詳細も追従
            repo.users.value = listOf(alice.copy(name = "Alice Updated"), bob)
            assertEquals(UserDetailState.Loaded("Alice Updated", isFavorite = false), awaitItem())
        }
    }

    @Test
    fun `詳細の局所Action(toggleFavorite)がstateに反映される`() = runTest {
        val shared = SharedState<String?>(alice.id)
        val repo = FakeUserRepository().apply { users.value = listOf(alice) }
        val detail = UserDetailStateHolder(shared, repo, backgroundScope)

        detail.state.test {
            skipItems(1) // NothingSelected
            assertEquals(UserDetailState.Loaded("Alice", isFavorite = false), awaitItem())

            detail.action.toggleFavorite()
            assertEquals(UserDetailState.Loaded("Alice", isFavorite = true), awaitItem())
        }
    }

    @Test
    fun `純変換(toListState)はholderを組み立てずに直接テストできる`() {
        val state = toListState(
            UserListSource(
                local = UserListLocal(query = ""),
                users = listOf(alice, bob),
                selectedId = alice.id,
            ),
        )
        assertTrue(state.rows.single { it.id == alice.id }.isSelected)
    }
}
