plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.android.library)
    alias(libs.plugins.dokka)
    `maven-publish`
    signing
}

description = "Core StateHolder implementation for Kotlin Multiplatform"

android {
    namespace = "io.github.rentoyokawa.stateholder"
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
            baseName = "StateHolderCore"
        }
    }
    
    sourceSets {
        commonMain {
            dependencies {
                api(libs.kotlinx.datetime)
                api(libs.kotlinx.serialization.json)
                implementation(libs.kotlinx.coroutines.core)
            }
        }
        
        commonTest {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.kotlinx.coroutines.test)
                implementation("app.cash.turbine:turbine:1.1.0")
            }
        }
    }
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
    
    publications.withType<MavenPublication> {
        artifactId = "stateholder-core"
        
        pom {
            name.set("StateHolder KMP Core")
            description.set(project.description)
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