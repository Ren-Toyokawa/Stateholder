package test

import app.cash.turbine.test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.koin.core.Koin
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import test.generated.stateHolderModule_kspTest
import kotlin.test.assertEquals
import kotlin.test.assertIs

/**
 * KSP が実際に生成した Koin モジュール（[stateHolderModule_kspTest]）をランタイムで起動し、
 * [UserListStateHolder] と [UserDetailStateHolder] が同一の `SharedState<String?>` インスタンスで
 * 結線されていることを検証する統合テスト（[[STA-14]] 受入基準の核）。
 *
 * 生成テンプレートは `CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)` を使うため、
 * JVM テスト環境では [Dispatchers.setMain] で Main ディスパッチャを用意しておく必要がある。
 */
@OptIn(ExperimentalCoroutinesApi::class)
class GeneratedModuleIntegrationTest {

    private val alice = User("u1", "Alice")
    private val bob = User("u2", "Bob")

    private lateinit var koin: Koin

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        koin = startKoin {
            modules(
                stateHolderModule_kspTest,
                module { single { FakeUserRepository().apply { users.value = listOf(alice, bob) } } },
            )
        }.koin
    }

    @After
    fun tearDown() {
        stopKoin()
        Dispatchers.resetMain()
    }

    @Test
    fun `生成されたfactoryで解決したVMの2holderは同一SharedStateを共有し一覧のselectが詳細に伝播する`() = runTest {
        val vm = koin.get<UserViewModel>()

        // 一覧側の select が詳細側の state に伝播するのは、生成された factory が
        // list / detail 両 holder に同一の SharedState インスタンスを渡している場合のみ。
        vm.detail.state.test {
            assertEquals(UserDetailState.NothingSelected, awaitItem())

            vm.list.action.select(alice.id)

            val loaded = assertIs<UserDetailState.Loaded>(awaitItem())
            assertEquals("Alice", loaded.name)
        }
    }

    @Test
    fun `koinから2回解決したVMはそれぞれ独立したSharedStateを持つ(factoryはVMごとに新規構築)`() = runTest {
        val vm1 = koin.get<UserViewModel>()
        val vm2 = koin.get<UserViewModel>()

        vm1.detail.state.test {
            assertEquals(UserDetailState.NothingSelected, awaitItem())
            vm1.list.action.select(alice.id)
            assertIs<UserDetailState.Loaded>(awaitItem())
        }

        // vm1 で選択済みでも、vm2 は独立した SharedState インスタンスを持つため未選択のまま
        vm2.detail.state.test {
            assertEquals(UserDetailState.NothingSelected, awaitItem())
        }
    }
}
