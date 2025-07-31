// settings.gradle.kts (Root Project)
// This file defines the modules that are part of your project.

// Enable feature preview for type-safe project accessors.
// This allows us to reference modules like `project.shared` in a type-safe way.
@file:Suppress("UnstableApiUsage")

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // --- FIX: Add the JitPack repository for MPAndroidChart ---
        maven { url = uri("https://jitpack.io") }
    }
    // Gradle automatically discovers and configures the 'libs' version catalog
    // from the 'gradle/libs.versions.toml' file. No explicit configuration is needed here.
}

rootProject.name = "Finlight"

// Include the new modules in the project.
// The androidApp module holds the Android-specific UI and code.
// The shared module will hold the common Kotlin business logic.
include(":androidApp")
include(":shared")
