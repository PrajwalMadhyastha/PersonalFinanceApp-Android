// In settings.gradle.kts (the one in your project's root directory)

pluginManagement {
    repositories {
        // This simpler format is less restrictive and should resolve the issue.
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
    }
}

rootProject.name = "PersonalFinanceApp"
include(":app")