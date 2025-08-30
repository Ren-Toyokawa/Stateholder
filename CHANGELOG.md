# Changelog

All notable changes to the StateHolder KMP project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [0.1.0-alpha01] - 2024-08-30

### Added
- Initial alpha release of StateHolder KMP library
- Core StateHolder implementation for state management
- KSP processor with Koin dependency injection support
- ViewModel integration for Android and Compose Multiplatform
- Annotations for code generation (`@StateHolder`, `@SharedState`, `@InjectedParam`)
- Support for Kotlin Multiplatform targets:
  - Android (minSdk 24)
  - JVM (target 17)
  - iOS (iosX64, iosArm64, iosSimulatorArm64)
- XCFramework generation for iOS integration
- Maven Central publishing with GPG signing
- Comprehensive documentation in English and Japanese

### Features
- **StateHolder Pattern**: Clean separation of UI state management from ViewModels
- **SharedState**: Cross-StateHolder state sharing within ViewModel scope
- **Code Generation**: Automatic parameter provider and Koin module generation
- **Type Safety**: Full Kotlin type safety with compile-time validation
- **Testing Support**: Comprehensive test utilities and examples

### Technical Details
- Kotlin 2.0.10
- Coroutines 1.8.1 
- KSP 2.0.10-1.0.24
- Koin 3.5.6
- Built with standard Gradle `maven-publish` plugin

### Published Modules
- `io.github.rentoyokawa:stateholder-annotations:0.1.0-alpha01`
- `io.github.rentoyokawa:stateholder-core:0.1.0-alpha01`
- `io.github.rentoyokawa:stateholder-viewmodel-koin:0.1.0-alpha01`
- `io.github.rentoyokawa:stateholder-processor-koin:0.1.0-alpha01`

[Unreleased]: https://github.com/Ren-Toyokawa/stateholder-kmp/compare/v0.1.0-alpha01...HEAD
[0.1.0-alpha01]: https://github.com/Ren-Toyokawa/stateholder-kmp/releases/tag/v0.1.0-alpha01