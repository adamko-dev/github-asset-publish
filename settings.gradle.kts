@file:Suppress("UnstableApiUsage")

rootProject.name = "github-asset-publish"

pluginManagement {
  repositories {
    mavenCentral()
    gradlePluginPortal()
  }
}

dependencyResolutionManagement {
  repositoriesMode = RepositoriesMode.PREFER_SETTINGS
  repositories {
    mavenCentral()
    gradlePluginPortal()
  }
}

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

include(
  ":modules:gradle-plugin-publisher",
  ":modules:gradle-plugin-repository",
  ":modules:lib-asset-uploader",
  ":modules:lib-asset-uploader-api",
  ":modules:lib-gmm",
//  ":modules:lib-gmm-rewriter",
  ":modules:maven-plugin-publisher",
  ":modules:maven-plugin-repository",
)
