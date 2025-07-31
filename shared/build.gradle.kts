// shared/build.gradle.kts
// This file configures the shared Kotlin Multiplatform module.

@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library) // Apply Android Library plugin for androidMain
    alias(libs.plugins.sqlDelight)
}

kotlin {
    // Define the targets for our multiplatform module.
    // We'll build for Android and iOS.
    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = "17"
            }
        }
    }
    iosX64()
    iosArm64()
    iosSimulatorArm64()

    // Define the source sets where our code will live.
    sourceSets {
        val commonMain by getting {
            dependencies {
                // Dependencies for all platforms will go here.
                // e.g., Coroutines, Ktor, SQLDelight runtime
            }
        }
        val androidMain by getting {
            dependencies {
                // Android-specific dependencies
            }
        }
        val iosMain by getting {
            // iOS-specific dependencies
        }
    }
}

android {
    namespace = "io.pm.finlight.shared"
    compileSdk = libs.versions.compileSdk.get().toInt()
    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

sqldelight {
    databases {
        create("AppDatabase") {
            packageName.set("io.pm.finlight.shared.db")
        }
    }
}

