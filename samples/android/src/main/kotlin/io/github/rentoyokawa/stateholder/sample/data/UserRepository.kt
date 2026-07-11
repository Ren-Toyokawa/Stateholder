package io.github.rentoyokawa.stateholder.sample.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

data class User(
    val id: String,
    val name: String,
    val bio: String,
)

/**
 * インメモリ実装のリポジトリ。
 * 実アプリでは Room / API クライアント等に置き換わる想定。
 */
class UserRepository {
    private val _users = MutableStateFlow(
        listOf(
            User("u1", "Alice", "Android engineer. Loves Compose."),
            User("u2", "Bob", "Backend developer. Kotlin server-side."),
            User("u3", "Carol", "Designer. Figma wizard."),
            User("u4", "Dave", "QA engineer. Breaks things professionally."),
        ),
    )

    val users: StateFlow<List<User>> = _users.asStateFlow()

    fun user(id: String): Flow<User?> = users.map { list -> list.find { it.id == id } }
}
