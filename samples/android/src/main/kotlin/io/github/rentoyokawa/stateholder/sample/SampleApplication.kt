package io.github.rentoyokawa.stateholder.sample

import android.app.Application
import io.github.rentoyokawa.stateholder.sample.data.UserRepository
import io.github.rentoyokawa.stateholder.sample.di.generated.stateHolderModule_sample
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.dsl.module

/**
 * DI ルート。KSP 生成の Koin モジュール（[stateHolderModule_sample]）と
 * [UserRepository] の `single` 定義を読み込む。
 *
 * `stateHolderModule_sample` は `samples/android/build.gradle.kts` の
 * `ksp { arg("stateholder.module.suffix", "sample"); arg("stateholder.module.package", ...) }`
 * で固定したプロパティ名・パッケージから import している（KSP arg 未指定時は非決定 uniqueId になるため必須）。
 */
class SampleApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@SampleApplication)
            modules(
                stateHolderModule_sample,
                module { single { UserRepository() } },
            )
        }
    }
}
