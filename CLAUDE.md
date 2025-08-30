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
./gradlew :stateholder-processor-koin:test

# Clean build artifacts
./gradlew clean

# Build specific modules
./gradlew :stateholder-core:build
./gradlew :stateholder-processor-koin:build
./gradlew :stateholder-annotations:build
```

## Architecture

### Module Structure

- **stateholder-annotations**: Contains annotation definitions (`@StateHolder`, `@SharedState`, `@InjectedParam`, `@InjectStateHolder`)
- **stateholder-core**: Core StateHolder abstract class and base functionality
- **stateholder-viewmodel**: ViewModel extensions and state holder delegation
- **stateholder-viewmodel-koin**: ViewModel extensions and state holder delegation with Koin integration
- **stateholder-processor-koin**: KSP processor for code generation (generates parameter providers and Koin modules)
- **stateholder-processor-koin-test**: Test module for KSP processor validation

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

The processor (`stateholder-processor-koin` module) handles:
- Detection of annotated classes
- Validation of StateHolder implementations
- Generation of parameter providers and Koin modules
- Located in `io.github.rentoyokawa.stateholder.ksp` package

### Testing

- Unit tests use `kotlin-compile-testing` for KSP processor validation
- Core module uses Turbine for Flow testing
- Test snapshots stored in `stateholder-processor-koin/src/test/resources/snapshots/`

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

## README Management Guidelines

This project maintains both Japanese and English versions of README files:

- **README.ja.md** (Primary): The main documentation in Japanese
- **README.md** (Secondary): English version synchronized from Japanese

### Workflow for README Updates

1. **Make changes to README.ja.md**: All content modifications should be made to the Japanese version first
2. **Sync to English**: Use the custom slash command to translate and sync to README.md:
   ```
   /sync-readme
   ```
3. **Review the translation**: Manually review and adjust the English version if needed

### Why This Approach

- README.ja.md serves as the single source of truth
- Automated translation ensures consistency in structure and content
- Manual review step allows for cultural and linguistic adjustments
- Reduces maintenance overhead while keeping both versions current

## Git Commit Guidelines

### Commit Messages
- Write clear, concise commit messages in English
- Use conventional commit format when applicable
- **Do NOT include** "Generated with [Claude Code]" signatures
- Focus on what was changed and why

### Pull Requests
- Create comprehensive PR descriptions explaining the changes
- **Do NOT include** "Generated with [Claude Code]" signatures
- Include testing information and breaking changes if any
- Reference related Linear issues when applicable