pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
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

rootProject.name = "ProjectEternal"

include(":app")
include(":core-model")
include(":core-simulation")
include(":core-content")
include(":core-persistence")
include(":feature-character")
include(":feature-adventure")
include(":feature-industry")
include(":feature-economy")
