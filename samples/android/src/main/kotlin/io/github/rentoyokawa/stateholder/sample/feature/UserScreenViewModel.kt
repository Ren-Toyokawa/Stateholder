package io.github.rentoyokawa.stateholder.sample.feature

import androidx.lifecycle.ViewModel
import io.github.rentoyokawa.stateholder.core.StateHolder

/**
 * ViewModel は具象 holder を「コンストラクタで受け取り、契約型で公開するだけ」。
 * ロジックは各 StateHolder（= ViewModel の一片）に分割されている。
 *
 * KSP（stateholder-processor-koin, STA-14）は本クラスのコンストラクタが
 * `@StateHolder` 具象クラス（[UserListStateHolder] / [UserDetailStateHolder]）を
 * 受け取ることを検出し、Koin `factory<UserScreenViewModel> { }` を生成する。
 * DI は具象・View 境界（[userList] / [userDetail] の公開型）は契約という STA-13 の決定に従う。
 *
 * scope は生成 factory 内でローカルに作られ本 VM には渡らないため、
 * scope の生成・破棄ライフサイクル（[[STA-15]]）は本チケットの範囲外。
 */
class UserScreenViewModel(
    userList: UserListStateHolder,
    userDetail: UserDetailStateHolder,
) : ViewModel() {

    val userList: StateHolder<UserListState, UserListAction> = userList
    val userDetail: StateHolder<UserDetailState, UserDetailAction> = userDetail
}
