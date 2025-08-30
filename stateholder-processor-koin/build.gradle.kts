plugins {
    alias(libs.plugins.kotlin.jvm)
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll(
            "-opt-in=com.tschuchort.compiletesting.ExperimentalCompilerApi"
        )
        allWarningsAsErrors = false
    }
}

dependencies {
    // KSP API（プロセッサー作成用）
    implementation(libs.ksp.api)
    
    // コード生成用
    implementation(libs.kotlinpoet)
    
    // アノテーション定義（型安全なアクセスのため）
    implementation(project(":stateholder-annotations"))
    
    // テスト環境でKSPプロセッサーを適用
    testImplementation(libs.kotlin.compile.testing)
    testImplementation(libs.kotlin.compile.testing.ksp)
    testCompileOnly(libs.lifecycle.viewmodel.compose)
    testCompileOnly(libs.kotlinx.coroutines.core)

    // テスト用
    testImplementation(kotlin("test"))
    testImplementation(libs.kotlinpoet)
    testImplementation(libs.mockk)
}