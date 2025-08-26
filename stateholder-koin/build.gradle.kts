plugins {
    kotlin("multiplatform")
    id("com.android.library")
}

android {
    namespace = "io.github.rentoyokawa.stateholder.koin"
    compileSdk = 34
    
    defaultConfig {
        minSdk = 24
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = "17"
            }
        }
    }
    
    jvm {
        compilations.all {
            kotlinOptions {
                jvmTarget = "17"
            }
        }
    }
    
    iosX64()
    iosArm64()
    iosSimulatorArm64()
    
    sourceSets {
        commonMain {
            dependencies {
                // StateHolderモジュールへの依存
                api(project(":stateholder-core"))
                api(project(":stateholder-annotations"))
                
                // Koin Core依存関係
                implementation("io.insert-koin:koin-core:3.5.6")
                implementation("io.insert-koin:koin-core-coroutines:3.5.6")
                
                // Coroutines
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
            }
        }
        
        commonTest {
            dependencies {
                implementation(kotlin("test"))
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
                implementation("app.cash.turbine:turbine:1.1.0")
                
                // Koin Test
                implementation("io.insert-koin:koin-core:3.5.6")
            }
        }
        
        androidMain {
            dependencies {
                implementation("io.insert-koin:koin-android:3.5.6")
            }
        }
    }
}