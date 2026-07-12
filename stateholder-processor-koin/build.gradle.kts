plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.dokka)
    alias(libs.plugins.maven.publish)
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
    // KSType -> KotlinPoet TypeName 変換（SharedState<T> の T を型安全に扱うため）
    implementation(libs.kotlinpoet.ksp)

    // アノテーション定義（型安全なアクセスのため）
    implementation(project(":stateholder-annotations"))

    // テスト環境でKSPプロセッサーを適用（kspWithCompilation=true で生成コードを実コンパイルするため
    // 依存は testImplementation にする。testCompileOnly だと実行時クラスパスに乗らずコンパイルが失敗する）
    testImplementation(libs.kotlin.compile.testing)
    testImplementation(libs.kotlin.compile.testing.ksp)
    testImplementation(libs.lifecycle.viewmodel.compose)
    testImplementation(libs.kotlinx.coroutines.core)
    testImplementation(project(":stateholder-core"))
    testImplementation(libs.koin.core)

    // テスト用
    testImplementation(kotlin("test"))
    testImplementation(libs.kotlinpoet)
}


mavenPublishing {
    publishToMavenCentral(com.vanniktech.maven.publish.SonatypeHost.CENTRAL_PORTAL)
    signAllPublications()
    
    pom {
        name = "StateHolder Processor Koin"
        description = "KSP processor for StateHolder with Koin support"
        inceptionYear = "2024"
        url = "https://github.com/Ren-Toyokawa/stateholder-kmp"
        licenses {
            license {
                name = "The Apache License, Version 2.0"
                url = "https://www.apache.org/licenses/LICENSE-2.0.txt"
                distribution = "https://www.apache.org/licenses/LICENSE-2.0.txt"
            }
        }
        developers {
            developer {
                id = "rentoyokawa"
                name = "Ren Toyokawa"
                url = "https://github.com/Ren-Toyokawa/"
            }
        }
        scm {
            url = "https://github.com/Ren-Toyokawa/stateholder-kmp"
            connection = "scm:git:git://github.com/Ren-Toyokawa/stateholder-kmp.git"
            developerConnection = "scm:git:ssh://git@github.com/Ren-Toyokawa/stateholder-kmp.git"
        }
    }
}