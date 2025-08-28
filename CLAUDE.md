# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

StateHolder KMP is a Kotlin Multiplatform library for state management with KSP code generation. It provides a clean architecture pattern separating UI state management from ViewModels for better testability and reusability.

## Build Commands

```bash
# Build the entire project
./gradlew build

# Run all tests
./gradlew test

# Run tests for specific module
./gradlew :stateholder-core:test
./gradlew :stateholder-ksp:test

# Clean build artifacts
./gradlew clean

# Build specific modules
./gradlew :stateholder-core:build
./gradlew :stateholder-ksp:build
./gradlew :stateholder-annotations:build
```

## Architecture

### Module Structure

- **stateholder-annotations**: Contains annotation definitions (`@StateHolder`, `@SharedState`, `@InjectedParam`, `@InjectStateHolder`)
- **stateholder-core**: Core StateHolder abstract class and base functionality
- **stateholder-viewmodel**: ViewModel extensions and state holder delegation
- **stateholder-ksp**: KSP processor for code generation (generates parameter providers and Koin modules)
- **stateholder-koin**: Koin integration module (currently empty, likely for future features)
- **stateholder-ksp-test**: Test module for KSP processor validation

### Key Design Patterns

1. **StateHolder Pattern**: Encapsulates state management logic separately from ViewModels
   - Extends `StateHolder<Source, State, Action>` abstract class
   - `Source`: Internal state representation
   - `State`: UI state exposed to views
   - `Action`: Interface for user actions

2. **SharedState**: States shared between multiple StateHolders within ViewModel scope
   - Marked with `@SharedState` annotation in ViewModels
   - Automatically injected into StateHolders via `@InjectedParam`

3. **Code Generation**: KSP processor generates:
   - Parameter providers for automatic StateHolder resolution
   - Koin modules for dependency injection setup

### Data Flow Architecture

```
UI → ViewModel → StateHolder → Repository/UseCase → DataSource
         ↓
    SharedState (cross-StateHolder communication)
```

## Development Guidelines

### When Adding New StateHolders

1. Annotate with `@StateHolder`
2. Define constructor parameters with `@InjectedParam` for SharedStates
3. Implement abstract methods:
   - `defineState(source: Source): State` - Pure transformation function
   - `createStateFlow(): Flow<Source>` - Combine data sources
   - `createInitialState(): State` - Initial UI state
   - `action: Action` - User action handlers

### When Working with KSP Processor

The processor (`stateholder-ksp` module) handles:
- Detection of annotated classes
- Validation of StateHolder implementations
- Generation of parameter providers and Koin modules
- Located in `io.github.rentoyokawa.stateholder.ksp` package

### Testing

- Unit tests use `kotlin-compile-testing` for KSP processor validation
- Core module uses Turbine for Flow testing
- Test snapshots stored in `stateholder-ksp/src/test/resources/snapshots/`

## Platform Support

- Android (minSdk 24, compileSdk 34)
- iOS (iosX64, iosArm64, iosSimulatorArm64)
- JVM (target 17)

## Dependencies

- Kotlin 2.0.10
- KSP 2.0.10-1.0.24
- Coroutines 1.8.1
- Koin 3.5.6
- KotlinPoet 1.16.0 (for code generation)