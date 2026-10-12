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

plugins { id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0" }

dependencyResolutionManagement {
  // Not FAIL_ON_PROJECT_REPOS: the Kotlin/Wasm plugin registers the Node.js
  // toolchain download as a project repository during configuration, which that
  // mode rejects outright.
  repositories {
    google()
    mavenCentral()
  }
}

rootProject.name = "openhands-cloud"

include(":app")
include(":shared")
include(":server")
include(":web")
