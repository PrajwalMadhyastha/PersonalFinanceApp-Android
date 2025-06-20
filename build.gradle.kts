plugins {
    id("com.android.application") version "8.4.1" apply false
    // Set the Kotlin version for the entire project.
    id("org.jetbrains.kotlin.android") version "2.0.0" apply false
    // Set the KSP version for the entire project.
    id("com.google.devtools.ksp") version "2.0.0-1.0.21" apply false
    // --- ADDED: Declare the Compose Compiler plugin for the project ---
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.0" apply false
}