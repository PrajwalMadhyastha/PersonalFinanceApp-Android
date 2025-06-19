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
        // This line is essential for downloading third-party libraries like Vico.
        mavenCentral()
        maven("https://jitpack.io")
    }
}
rootProject.name = "Personal Finance App"
include(":app")
