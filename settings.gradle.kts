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
include(":stateholder-processor-koin")
include(":stateholder-processor-koin-test")
include(":samples:android")