package test

import androidx.lifecycle.ViewModel

/**
 * KSP 生成の統合テスト用 ViewModel。
 *
 * コンストラクタで [UserListStateHolder] と [UserDetailStateHolder] を受け取る。
 * KSP プロセッサーはこのクラスを検出し、Koin `factory { }` 定義を生成する
 * （`selectedUserId: SharedState<String?>` を要求する両 holder には同一インスタンスが渡る）。
 */
class UserViewModel(
    val list: UserListStateHolder,
    val detail: UserDetailStateHolder,
) : ViewModel()
