// build.gradle.kts (Root Project)
// This file defines the plugins and configurations that apply to the entire project.

@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    // --- FIX: Use the corrected, non-nested plugin aliases ---
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.ktlint) apply false
    alias(libs.plugins.sqlDelight) apply false
}

// Apply the Ktlint plugin to all subprojects to maintain code style.
allprojects {
    apply(plugin = "org.jlleitschuh.gradle.ktlint")
}
