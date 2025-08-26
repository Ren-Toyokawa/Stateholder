plugins {
    kotlin("jvm")
    id("com.google.devtools.ksp")
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
    ksp(project(":stateholder-ksp"))
    
    // Dependencies for processor
    implementation(project(":stateholder-annotations"))
    implementation(project(":stateholder-core"))
    implementation(project(":stateholder-viewmodel"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
    implementation("io.insert-koin:koin-core:3.5.6")
    implementation("androidx.lifecycle:lifecycle-viewmodel:2.8.3")
    
    // Test
    testImplementation(kotlin("test"))
    testImplementation("io.insert-koin:koin-test:3.5.6")
    testImplementation("io.insert-koin:koin-test-junit4:3.5.6")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
    testImplementation("junit:junit:4.13.2")
}

ksp {
    arg("ENABLE_LOGGING", "true")
    arg("stateholder.module.suffix", "kspTest")
}