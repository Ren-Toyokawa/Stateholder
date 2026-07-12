package test

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * KSP 生成の統合テスト用フェイクリポジトリ。
 * Koin から `get()` で解決される「その他の依存」の実例。
 */
data class User(val id: String, val name: String)

class FakeUserRepository {
    val users = MutableStateFlow<List<User>>(emptyList())
    fun user(id: String): Flow<User?> = users.map { list -> list.find { it.id == id } }
}
