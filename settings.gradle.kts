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
        maven("https://artifactory-external.vkpartner.ru/artifactory/maven/")
        maven("https://maven.pkg.jetbrains.space/yandex/public/maven")
        maven("https://jitpack.io")
    }
}

rootProject.name = "AgroSocial"
include(":app")
