// shared/build.gradle.kts
// This file configures the shared Kotlin Multiplatform module.

@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.sqlDelight)
    // --- FIX: Apply the Kotlin Serialization plugin ---
    alias(libs.plugins.kotlin.serialization)
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
                implementation(libs.sqlDelight.runtime)
                // --- FIX: Add the serialization library dependency ---
                implementation(libs.kotlinx.serialization.json)
            }
        }
        val androidMain by getting {
            dependencies {
                implementation(libs.sqlDelight.driver.android)
            }
        }

        val iosMain by creating {
            dependsOn(commonMain)
            dependencies {
                implementation(libs.sqlDelight.driver.native)
            }
        }
        val iosX64Main by getting {
            dependsOn(iosMain)
        }
        val iosArm64Main by getting {
            dependsOn(iosMain)
        }
        val iosSimulatorArm64Main by getting {
            dependsOn(iosMain)
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
