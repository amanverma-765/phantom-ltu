# GitHub Actions CI/CD with Release Signing

## Overview

Set up GitHub Actions for continuous integration and automated release publishing with proper APK signing for PhantomLTU.

## Components

### 1. Keystore Generation (One-time Setup)

Generate a release keystore locally and store it as GitHub Secrets.

**Secrets required:**
- `KEYSTORE_BASE64` — base64-encoded `.jks` file
- `KEYSTORE_PASSWORD` — store password
- `KEY_ALIAS` — key alias (e.g., `phantom-release`)
- `KEY_PASSWORD` — key password

### 2. CI Build Workflow — `.github/workflows/build.yml`

**Triggers:**
- Push to `main` and `phantom` branches
- Pull requests targeting `main`

**Job: build**
1. Checkout with recursive submodules (needed for `core/`)
2. Set up JDK 21
3. Set up Gradle with build cache
4. Run `./gradlew assembleDebug`
6. Run `./gradlew app:testDebugUnitTest`

### 3. Release Workflow — `.github/workflows/release.yml`

**Triggers:**
- Tag push matching `v*`
- Manual `workflow_dispatch` with optional `tag_name` input

**Job: release**
1. Checkout with recursive submodules
2. Set up JDK 21
3. Set up Gradle with build cache
4. Decode `KEYSTORE_BASE64` secret to `$RUNNER_TEMP/release.jks`
5. Build release APK:
   ```
   ./gradlew buildRelease \
     -PandroidStoreFile=$RUNNER_TEMP/release.jks \
     -PandroidStorePassword=*** \
     -PandroidKeyAlias=*** \
     -PandroidKeyPassword=***
   ```
7. Verify APK signature with `apksigner verify --verbose --print-certs`
8. Generate SHA256 checksum: `sha256sum phantom-ltu-*.apk > checksums-sha256.txt`
9. Determine tag name (from `GITHUB_REF_NAME` for tag triggers, or `tag_name` input for manual)
10. Create GitHub Release via `gh release create` with `--generate-notes`, attach APK + checksum
11. Clean up keystore file from runner

### 4. Signing Integration

The existing `build.gradle.kts` already reads signing properties:
```kotlin
signingConfigs.create("config") {
    val androidStoreFile = project.findProperty("androidStoreFile") as String?
    if (!androidStoreFile.isNullOrEmpty()) {
        storeFile = rootProject.file(androidStoreFile)
        storePassword = project.property("androidStorePassword") as String
        keyAlias = project.property("androidKeyAlias") as String
        keyPassword = project.property("androidKeyPassword") as String
    }
}
```
No build script changes needed. The workflow passes values via `-P` flags.

### 5. Security

- Keystore decoded to `$RUNNER_TEMP` (ephemeral) and deleted in a `post` step
- GitHub automatically masks secret values in logs
- `apksigner verify` confirms valid signature before publishing
- Fork PRs can still build but cannot release

## Files to Create

1. `.github/workflows/build.yml`
2. `.github/workflows/release.yml`

## Files Modified

None. Existing build scripts already support signing via Gradle properties.
