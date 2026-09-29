# Migrating to Built-in Kotlin in AGP 9

Starting with Android Gradle Plugin 9.0, Kotlin compilation is integrated directly into AGP.

## 1. Remove the Kotlin Android Plugin

Remove the `org.jetbrains.kotlin.android` plugin from module-level `build.gradle.kts` files:

```kotlin
plugins {
    alias(libs.plugins.android.application)
    // REMOVE: alias(libs.plugins.kotlin.android)
}
```

AGP will automatically apply and configure Kotlin compilation for Android source sets.

## 2. Kotlin Compiler Configuration

Configure Kotlin options via the `android.kotlin` block instead of top-level `kotlin`:

```kotlin
android {
    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
            freeCompilerArgs.addAll("-opt-in=kotlin.RequiresOptIn")
        }
    }
}
```

## 3. Clean up gradle.properties

Remove any deprecated transition flags from `gradle.properties`:
- `android.builtInKotlin=true`
- `android.disallowKotlinSourceSets`
