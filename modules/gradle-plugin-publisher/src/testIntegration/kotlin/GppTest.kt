package dev.adamko.githubassetpublish

import java.nio.file.Path
import kotlin.io.path.*
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Assertions.assertLinesMatch
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class GppTest {

  @Test
  fun testJavaLibraryProject(
    @TempDir
    projectDir: Path
  ) {
    projectDir.resolve("gradle.properties").writeText(
      """
      |org.gradle.jvmargs=-Dfile.encoding=UTF-8
      |org.gradle.configuration-cache=true
      |org.gradle.parallel=true
      |org.gradle.caching=true
      |""".trimIndent()
    )

    projectDir.resolve("settings.gradle.kts").writeText(
      """
      |rootProject.name = "test-project"
      |
      |pluginManagement {
      |  repositories {
      |    maven(file("${devMavenRepo.invariantSeparatorsPathString}"))
      |    mavenCentral()
      |    gradlePluginPortal()
      |  }
      |}
      |dependencyResolutionManagement {
      |  repositoriesMode = RepositoriesMode.PREFER_SETTINGS
      |  repositories {
      |    maven(file("${devMavenRepo.invariantSeparatorsPathString}"))
      |    mavenCentral()
      |    gradlePluginPortal()
      |  }
      |}
      |""".trimMargin()
    )

    projectDir.resolve("build.gradle.kts").writeText(
      """
      |plugins {
      |  `java-library`
      |  id("dev.adamko.github-asset-publisher") version "+"
      |  `maven-publish`
      |}
      |
      |group = "com.example"
      |version = "1.0.0"
      |
      |publishing {
      |  publications {
      |    register<MavenPublication>("maven") {
      |      from(components["java"])
      |    }
      |  }
      |}
      |
      |tasks.withType<dev.adamko.githubassetpublish.tasks.UploadGitHubReleaseAssetsTask>().configureEach {
      |  githubRepo.set("aSemy/demo-github-asset-publish-repo")
      |}
      |""".trimMargin()
    )

    projectDir.resolve("src/main/java/com/example/Demo.java")
      .createParentDirectories()
      .writeText(
        """
        |package com.example;
        |
        |public class Demo {}
        |""".trimMargin()
      )

    GradleRunner.create()
      .withProjectDir(projectDir.toFile())
      .withArguments(
        "uploadGitHubReleaseAssets",
        "--stacktrace",
      )
      .build()

    val githubReleaseFilesDir = projectDir.resolve("build/tmp/uploadGitHubReleaseAssets/")

    val githubReleaseFiles = githubReleaseFilesDir
      .walk()
      .sorted()
      .map { f ->
        val path = f.relativeTo(githubReleaseFilesDir).invariantSeparatorsPathString
        buildString {
          append(path)
          if (!f.isRegularFile()) {
            append(" ${f.describeType()}")
          }
        }
      }
      .toList()

    assertLinesMatch(
      listOf(
        "com.example/test-project/test-project-1.0.0.ivy.xml",
        "com.example/test-project/test-project-1.0.0.jar",
        "com.example/test-project/test-project-1.0.0.module",
        "com.example/test-project/test-project-1.0.0.module.sha256",
        "com.example/test-project/test-project-1.0.0.module.sha512",
      ),
      githubReleaseFiles,
    )
  }

  @Test
  fun testKotlinMultiplatformProject(
    @TempDir
    projectDir: Path
  ) {
    projectDir.resolve("gradle.properties").writeText(
      """
      |org.gradle.jvmargs=-Dfile.encoding=UTF-8
      |org.gradle.configuration-cache=true
      |org.gradle.parallel=true
      |org.gradle.caching=true
      |""".trimIndent()
    )

    projectDir.resolve("settings.gradle.kts").writeText(
      """
      |rootProject.name = "test-project"
      |
      |pluginManagement {
      |  repositories {
      |    maven(file("${devMavenRepo.invariantSeparatorsPathString}"))
      |    mavenCentral()
      |    gradlePluginPortal()
      |  }
      |}
      |dependencyResolutionManagement {
      |  repositoriesMode = RepositoriesMode.PREFER_SETTINGS
      |  repositories {
      |    maven(file("${devMavenRepo.invariantSeparatorsPathString}"))
      |    mavenCentral()
      |    gradlePluginPortal()
      |  }
      |}
      |""".trimMargin()
    )

    projectDir.resolve("build.gradle.kts").writeText(
      """
      |plugins {
      |  kotlin("multiplatform") version "2.2.21"
      |  id("dev.adamko.github-asset-publisher") version "+"
      |  `maven-publish`
      |}
      |
      |group = "com.example"
      |version = "1.0.0"
      |
      |kotlin {
      |  jvm()
      |  js { browser() }
      |  linuxX64()
      |}
      |
      |tasks.withType<dev.adamko.githubassetpublish.tasks.UploadGitHubReleaseAssetsTask>().configureEach {
      |  githubRepo.set("aSemy/demo-github-asset-publish-repo")
      |}
      |
      |""".trimMargin()
    )

    projectDir.resolve("src/commonMain/kotlin/Demo.kt")
      .createParentDirectories()
      .writeText(
        """
        |package com.example
        |
        |val demo = "demo"
        |""".trimMargin()
      )

    GradleRunner.create()
      .withProjectDir(projectDir.toFile())
      .withArguments("uploadGitHubReleaseAssets")
      .build()

    val githubReleaseFilesDir = projectDir.resolve("build/tmp/uploadGitHubReleaseAssets/")

    val githubReleaseFiles = githubReleaseFilesDir
      .walk()
      .sorted()
      .map { f ->
        val path = f.relativeTo(githubReleaseFilesDir).invariantSeparatorsPathString
        buildString {
          append(path)
          if (!f.isRegularFile()) {
            append(" ${f.describeType()}")
          }
        }
      }
      .toList()

    assertLinesMatch(
      listOf(
//        "com.example/test-project/test-project-1.0.0-sources.jar",
//        "com.example/test-project/test-project-1.0.0.ivy.xml",
//        "com.example/test-project/test-project-1.0.0.jar",
//        "com.example/test-project/test-project-1.0.0.module",
//        "com.example/test-project/test-project-1.0.0.module.sha256",
//        "com.example/test-project/test-project-1.0.0.module.sha512",
//        "com.example/test-project/test-project-linuxx64-1.0.0-sources.jar",
//        "com.example/test-project/test-project-linuxx64-1.0.0.klib",
//        "com.example/test-project/test-project-linuxx64-1.0.0.module",
//        "com.example/test-project/test-project-linuxx64-1.0.0.module.sha256",
//        "com.example/test-project/test-project-linuxx64-1.0.0.module.sha512",
        "com.example/test-project-js/test-project-1.0.0.ivy.xml",
        "com.example/test-project-js/test-project-js-1.0.0-sources.jar",
        "com.example/test-project-js/test-project-js-1.0.0.klib",
        "com.example/test-project-js/test-project-js-1.0.0.module",
        "com.example/test-project-js/test-project-js-1.0.0.module.sha256",
        "com.example/test-project-js/test-project-js-1.0.0.module.sha512",
        "com.example/test-project-jvm/test-project-1.0.0.ivy.xml",
        "com.example/test-project-jvm/test-project-jvm-1.0.0-sources.jar",
        "com.example/test-project-jvm/test-project-jvm-1.0.0.jar",
        "com.example/test-project-jvm/test-project-jvm-1.0.0.module",
        "com.example/test-project-jvm/test-project-jvm-1.0.0.module.sha256",
        "com.example/test-project-jvm/test-project-jvm-1.0.0.module.sha512",
        "com.example/test-project-linuxx64/test-project-1.0.0.ivy.xml",
        "com.example/test-project-linuxx64/test-project-linuxx64-1.0.0-sources.jar",
        "com.example/test-project-linuxx64/test-project-linuxx64-1.0.0.klib",
        "com.example/test-project-linuxx64/test-project-linuxx64-1.0.0.module",
        "com.example/test-project-linuxx64/test-project-linuxx64-1.0.0.module.sha256",
        "com.example/test-project-linuxx64/test-project-linuxx64-1.0.0.module.sha512",
        "com.example/test-project/test-project-1.0.0-sources.jar",
        "com.example/test-project/test-project-1.0.0.ivy.xml",
        "com.example/test-project/test-project-1.0.0.jar",
        "com.example/test-project/test-project-1.0.0.module",
        "com.example/test-project/test-project-1.0.0.module.sha256",
        "com.example/test-project/test-project-1.0.0.module.sha512",
      ),
      githubReleaseFiles,
    )

    GradleRunner.create()
      .withProjectDir(projectDir.toFile())
      .withArguments(
        "uploadGitHubReleaseAssets",
        "--stacktrace",
      )
      .build()
      .let { result ->
        println(result.output)
      }

  }

  companion object {
    private val devMavenRepo: Path by lazy {
      System.getProperty("devMavenRepo")
        ?.let { Path(it) }
        ?: error("System property 'devMavenRepo' not set")
    }

    /**
     * Returns the type of file (file, directory, symlink, etc.).
     */
    internal fun Path.describeType(): String =
      buildString {
        append(
          when {
            !exists()       -> "<non-existent>"
            isRegularFile() -> "file"
            isDirectory()   -> "directory"
            else            -> "<unknown>"
          }
        )

        if (isSymbolicLink()) {
          append(" (symbolic link)")
        }
      }
  }
}
