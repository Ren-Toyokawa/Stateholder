plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.dokka)
    alias(libs.plugins.maven.publish)
}

description = "Annotations for StateHolder KMP code generation"

android {
    namespace = "io.github.rentoyokawa.stateholder.annotations"
    compileSdk = 34
    
    defaultConfig {
        minSdk = 24
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    
    publishing {
        singleVariant("release") {
            withSourcesJar()
            withJavadocJar()
        }
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
    
    // iOS Framework configuration
    targets.withType<org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget> {
        binaries.framework {
            baseName = "StateHolderAnnotations"
        }
    }
    
    sourceSets {
        commonMain {
            dependencies {
                // アノテーションのみのため、依存関係は不要
            }
        }
        
        commonTest {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}

mavenPublishing {
    publishToMavenCentral(com.vanniktech.maven.publish.SonatypeHost.CENTRAL_PORTAL)
    signAllPublications()
    
    pom {
        name = "StateHolder Annotations"
        description = "Annotations for StateHolder KSP code generation"
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