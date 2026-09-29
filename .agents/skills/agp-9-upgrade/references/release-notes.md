# AGP 9.0 Migration and Release Highlights

Android Gradle Plugin 9.0 introduces a declarative DSL and built-in Kotlin compilation.

## Compatibility Matrix

| Component | Minimum Version | Recommended |
|---|---|---|
| Gradle | 9.0+ | 9.3+ |
| JDK | 17 | 21 |
| Kotlin | 2.0+ | 2.1+ |
| KSP | 2.3.6+ | Latest compatible |

## Key DSL Migrations

1. **Variant APIs**: Replace `applicationVariants.all` with the new Variant APIs (`androidComponents.onVariants`).
2. **Built-in Kotlin**: Module-level Kotlin compilation is managed directly by AGP without needing the `org.jetbrains.kotlin.android` plugin.
3. **BuildConfig**: Custom fields now use the dedicated DSL extension. See [references/buildconfig.md](buildconfig.md).
4. **Recipes and Patterns**: See [references/recipes.md](recipes.md) for concrete Gradle migration recipes.
