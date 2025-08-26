plugins {
    kotlin("multiplatform") version "2.0.10" apply false
    kotlin("android") version "2.0.10" apply false
    kotlin("jvm") version "2.0.10" apply false
    kotlin("plugin.serialization") version "2.0.10" apply false
    id("com.android.library") version "8.5.2" apply false
    id("com.google.devtools.ksp") version "2.0.10-1.0.24" apply false
}

allprojects {
    group = "io.github.rentoyokawa"
    version = "0.1.0-SNAPSHOT"
    
    repositories {
        google()
        mavenCentral()
    }
}

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}