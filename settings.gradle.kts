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
include(":stateholder-viewmodel-koin")
include(":stateholder-processor-koin")
include(":stateholder-processor-koin-test")