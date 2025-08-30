plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.dokka)
    `maven-publish`
    signing
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

java {
    withSourcesJar()
    withJavadocJar()
}

publishing {
    repositories {
        maven {
            name = "Central"
            val releaseRepoUrl = "https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/"
            val snapshotRepoUrl = "https://s01.oss.sonatype.org/content/repositories/snapshots/"
            url = uri(if (version.toString().endsWith("SNAPSHOT")) snapshotRepoUrl else releaseRepoUrl)
            credentials {
                username = System.getenv("MAVEN_CENTRAL_USERNAME") ?: providers.gradleProperty("mavenCentralUsername").orNull
                password = System.getenv("MAVEN_CENTRAL_PASSWORD") ?: providers.gradleProperty("mavenCentralPassword").orNull
            }
        }
    }
    
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            artifactId = "stateholder-processor-koin"
            
            pom {
                name.set("StateHolder KMP Processor Koin")
                description.set("KSP processor for StateHolder code generation with Koin support")
                inceptionYear.set(providers.gradleProperty("POM_INCEPTION_YEAR"))
                url.set(providers.gradleProperty("POM_URL"))
                
                licenses {
                    license {
                        name.set(providers.gradleProperty("POM_LICENSE_NAME"))
                        url.set(providers.gradleProperty("POM_LICENSE_URL"))
                        distribution.set(providers.gradleProperty("POM_LICENSE_DIST"))
                    }
                }
                
                developers {
                    developer {
                        id.set(providers.gradleProperty("POM_DEVELOPER_ID"))
                        name.set(providers.gradleProperty("POM_DEVELOPER_NAME"))
                        url.set(providers.gradleProperty("POM_DEVELOPER_URL"))
                    }
                }
                
                scm {
                    url.set(providers.gradleProperty("POM_SCM_URL"))
                    connection.set(providers.gradleProperty("POM_SCM_CONNECTION"))
                    developerConnection.set(providers.gradleProperty("POM_SCM_DEV_CONNECTION"))
                }
            }
        }
    }
}

signing {
    val signingKey = System.getenv("SIGNING_KEY") ?: providers.gradleProperty("signingInMemoryKey").orNull
    val signingPassword = System.getenv("SIGNING_PASSWORD") ?: providers.gradleProperty("signingInMemoryKeyPassword").orNull
    
    // Only require signing for releases and when keys are available
    isRequired = !version.toString().endsWith("SNAPSHOT") && signingKey != null
    
    if (signingKey != null && signingPassword != null) {
        useInMemoryPgpKeys(signingKey, signingPassword)
        sign(publishing.publications)
    }
}