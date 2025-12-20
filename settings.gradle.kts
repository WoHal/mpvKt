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
    maven(url = "https://www.jitpack.io")
    mavenCentral()

    flatDir {
      dirs("aars")
    }
  }
}

rootProject.name = "mpvKt"
include(":library")
include(":AutoMaterial3")
include(":sample")
