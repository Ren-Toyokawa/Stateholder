package io.github.rentoyokawa.stateholder.sample.feature

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.rentoyokawa.stateholder.core.SharedState
import io.github.rentoyokawa.stateholder.core.StateHolder
import io.github.rentoyokawa.stateholder.sample.data.UserRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

/**
 * ViewModel は StateHolder を「コンストラクタで受け取り、公開するだけ」。
 * ロジックは各 StateHolder（= ViewModel の一片）に分割されている。
 *
 * 消費側の型は契約 [StateHolder]<State, Action> のみ。Source は一切見えない。
 */
class UserScreenViewModel(
    val userList: StateHolder<UserListState, UserListAction>,
    val userDetail: StateHolder<UserDetailState, UserDetailAction>,
    private val holderScope: CoroutineScope,
) : ViewModel() {

    override fun onCleared() {
        holderScope.cancel()
    }

    companion object {
        /**
         * 組み立て（composition root）の手書き版。
         * 将来はこの形のコードを KSP が生成する想定（STA-14）。
         *
         * - SharedState は「同一 ViewModel 内の holder 群で同一インスタンス」を結線
         * - scope の生成・破棄の扱いは STA-15 で決定するまでの仮置き
         *   （先に scope を作って holder に配り、ViewModel が onCleared で cancel する）
         */
        val Factory = viewModelFactory {
            initializer {
                val holderScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
                val selectedUserId = SharedState<String?>(null)
                val repository = UserRepository()
                UserScreenViewModel(
                    userList = UserListStateHolder(selectedUserId, repository, holderScope),
                    userDetail = UserDetailStateHolder(selectedUserId, repository, holderScope),
                    holderScope = holderScope,
                )
            }
        }
    }
}
