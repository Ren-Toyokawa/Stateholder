plugins {
    kotlin("jvm")
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
    implementation("com.google.devtools.ksp:symbol-processing-api:2.0.10-1.0.24")
    
    // コード生成用
    implementation("com.squareup:kotlinpoet:1.16.0")
    
    // アノテーション定義（型安全なアクセスのため）
    implementation(project(":stateholder-annotations"))
    
    // テスト環境でKSPプロセッサーを適用
    testImplementation("com.github.tschuchortdev:kotlin-compile-testing:1.6.0")
    testImplementation("com.github.tschuchortdev:kotlin-compile-testing-ksp:1.6.0")
    testCompileOnly("org.jetbrains.androidx.lifecycle:lifecycle-viewmodel-compose:2.8.0")
    testCompileOnly("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")

    // テスト用
    testImplementation(kotlin("test"))
    testImplementation("com.squareup:kotlinpoet:1.16.0")
    testImplementation("io.mockk:mockk:1.13.8")
}