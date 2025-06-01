plugins {
  `kotlin-dsl`
  kotlin("plugin.serialization") version embeddedKotlinVersion
  `maven-publish`
}

group = rootProject.group
version = rootProject.version

dependencies {
  implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.1")
}

kotlin {
  compilerOptions {
    optIn.add("kotlin.io.path.ExperimentalPathApi")
  }
}

gradlePlugin {
  plugins {
    create("githubAssetPublish") {
      id = "dev.adamko.github-asset-publisher"
      implementationClass = "dev.adamko.githubassetpublish.GitHubAssetPublishPlugin"
    }
  }
}
