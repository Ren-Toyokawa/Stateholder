plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ksp)
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll(
            "-opt-in=com.google.devtools.ksp.experimental.ExperimentalKspApi"
        )
        allWarningsAsErrors = false
    }
}

dependencies {
    // KSP processor
    ksp(project(":stateholder-processor-koin"))
    
    // Dependencies for processor
    implementation(project(":stateholder-annotations"))
    implementation(project(":stateholder-core"))
    implementation(project(":stateholder-viewmodel-koin"))
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.koin.core)
    implementation(libs.lifecycle.viewmodel)
    
    // Test
    testImplementation(kotlin("test"))
    testImplementation(libs.koin.test)
    testImplementation(libs.koin.test.junit4)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.junit)
}

ksp {
    arg("ENABLE_LOGGING", "true")
    arg("stateholder.module.suffix", "kspTest")
}