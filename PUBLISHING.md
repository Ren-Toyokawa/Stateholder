# Publishing StateHolder KMP

This document explains the CI/CD-based publishing process for StateHolder KMP library to Maven Central.

## Overview

**All releases are automated through GitHub Actions** - no manual publishing from local development environment.

## Prerequisites

### Maven Central Account

1. Create an account at [central.sonatype.com](https://central.sonatype.com/)
2. Request access for the `io.github.rentoyokawa` namespace

### GPG Signing Key

1. Generate a GPG key pair:
   ```bash
   gpg --full-generate-key
   ```

2. Export the private key:
   ```bash
   gpg --armor --export-secret-keys YOUR_KEY_ID > signing-key.asc
   ```

3. Get the key ID:
   ```bash
   gpg --list-secret-keys --keyid-format LONG
   ```

## Repository Secrets

Set up the following secrets in GitHub repository settings:

- `MAVEN_CENTRAL_USERNAME`: Your Sonatype username
- `MAVEN_CENTRAL_PASSWORD`: Your Sonatype password (or token) 
- `SIGNING_KEY`: Contents of your `signing-key.asc` file
- `SIGNING_PASSWORD`: Passphrase for your GPG key

## Local Development

For local development, **only use Maven Local**:

```bash
# Build project
./gradlew clean build

# Publish to local Maven repository (no signing required)
./gradlew publishToMavenLocal

# Verify artifacts
ls -la ~/.m2/repository/io/github/rentoyokawa/
```

**Note**: Local publishing does not require signing - it's automatically skipped when signing keys are not available.

## Release Process

### CI/CD Release Process

1. Go to GitHub Actions in the repository
2. Select "Release" workflow
3. Click "Run workflow"
4. Enter the version number (e.g., `0.1.0-alpha01`)
5. Click "Run workflow"

The automated workflow will:
- Update version numbers in `build.gradle.kts`
- Build and test the project
- Sign artifacts using GPG keys from GitHub Secrets
- Publish to Maven Central S01 repository
- Create a git tag and GitHub release
- Push changes back to main branch

### Security Features

- **Environment-based signing**: Only signs artifacts when running in CI with proper secrets
- **Automatic prerelease detection**: Alpha/beta/rc versions are marked as prerelease in GitHub
- **Credential isolation**: No sensitive data stored in local development environment

## Published Modules

The following modules are published to Maven Central using standard Gradle `maven-publish` plugin:

- `io.github.rentoyokawa:stateholder-annotations` - Annotations for code generation
- `io.github.rentoyokawa:stateholder-core` - Core StateHolder implementation  
- `io.github.rentoyokawa:stateholder-viewmodel-koin` - ViewModel integration with Koin
- `io.github.rentoyokawa:stateholder-processor-koin` - KSP processor with Koin support

All modules are configured with:
- Sources JAR publication
- Javadoc JAR publication (for JVM modules)
- GPG signing for Maven Central requirements
- Proper POM metadata

## Usage in Projects

After publishing via CI/CD, users can add the library to their projects:

```kotlin
// In build.gradle.kts
dependencies {
    implementation("io.github.rentoyokawa:stateholder-core:0.1.0-alpha01")
    implementation("io.github.rentoyokawa:stateholder-viewmodel-koin:0.1.0-alpha01")
    ksp("io.github.rentoyokawa:stateholder-processor-koin:0.1.0-alpha01")
}
```

Each GitHub release will include the usage instructions with the specific version number.

## Troubleshooting

### Publication Issues

1. **Signing errors**: Verify GPG key and passphrase are correct
2. **Authentication errors**: Check Maven Central credentials
3. **Namespace errors**: Ensure you have access to `io.github.rentoyokawa`

### Local Testing

If local publishing fails, check:
- All modules build successfully
- No missing dependencies
- Gradle properties are properly set

### GitHub Actions

Check the workflow logs for detailed error messages. Common issues:
- Missing repository secrets
- Invalid GPG key format
- Network connectivity issues with Maven Central