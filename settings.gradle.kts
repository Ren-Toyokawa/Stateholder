rootProject.name = "stateholder-kmp"

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

include(":stateholder-annotations")
include(":stateholder-core")
include(":stateholder-viewmodel")
include(":stateholder-ksp")
include(":stateholder-ksp-test")
include(":stateholder-koin")