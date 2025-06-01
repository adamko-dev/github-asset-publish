package dev.adamko.githubassetpublish.tasks

import dev.adamko.githubassetpublish.internal.computeChecksum
import dev.adamko.githubassetpublish.internal.model.GradleModuleMetadata
import dev.adamko.githubassetpublish.internal.model.MutableGradleModuleMetadata
import dev.adamko.githubassetpublish.internal.model.MutableGradleModuleMetadata.Companion.saveTo
import java.nio.file.Path
import javax.inject.Inject
import kotlin.io.path.*
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.file.RelativePath
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity.RELATIVE
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault

@DisableCachingByDefault(because = "Only performs simple local file operations.")
abstract class PrepareGitHubReleaseFilesTask
@Inject
internal constructor(
  private val fs: FileSystemOperations,
) : DefaultTask() {

  /**
   * The output directory for this task.
   *
   * Will contain all files to be attached as assets to a GitHub Release.
   */
  @get:OutputDirectory
  abstract val destinationDirectory: DirectoryProperty

  /**
   * Output of the Maven Publish task.
   */
  @get:InputFiles
  @get:PathSensitive(RELATIVE)
  abstract val buildDirMavenDirectory: DirectoryProperty

  @TaskAction
  protected fun taskAction() {
    val repoDir = buildDirMavenDirectory.get().asFile.toPath()
    val destinationDir = destinationDirectory.get().asFile.toPath()

    logger.info("[$path] processing buildDirMavenRepo: $repoDir")

    relocateFiles(
      sourceDir = repoDir,
      destinationDir = destinationDir,
    )

    updateRelocatedFiles(destinationDir)

    logger.lifecycle("[$path] outputDir:${buildDirMavenDirectory.get().asFile.invariantSeparatorsPath}")
  }


  private fun Path.moduleMetadataFiles(): Sequence<Pair<Path, MutableGradleModuleMetadata>> =
    walk()
      .filter { it.isRegularFile() && it.extension == "module" }
      .mapNotNull { moduleFile ->
        try {
          val metadata = MutableGradleModuleMetadata.loadFrom(moduleFile)
          moduleFile to metadata
        } catch (ex: Exception) {
          logger.warn("[$path] failed to load moduleFile ${moduleFile.invariantSeparatorsPathString}", ex)
          null
        }
      }

  private fun relocateFiles(
    sourceDir: Path,
    destinationDir: Path,
  ) {
    fs.sync {
      into(destinationDir)
      from(sourceDir)

      include(
        "**/*.jar",
        "**/*.module",
        "**/*.klib",
      )
      exclude(
        "**/*.md5",
        "**/*.sha1",
        "**/*.sha256",
        "**/*.sha512",
        "**/maven-metadata.xml",
        "**/*.pom",
      )

      eachFile {
        // remove directories
        relativePath = RelativePath(true, sourceName)
      }

      includeEmptyDirs = false
    }
  }

  private fun updateRelocatedFiles(
    destinationDir: Path,
  ) {
    destinationDir.moduleMetadataFiles().forEach { (moduleFile, metadata) ->
      updateModuleMetadata(moduleFile, metadata)
      createIvyModuleFile(moduleFile, metadata)
      createModuleChecksums(moduleFile)
    }
  }

  private fun updateModuleMetadata(
    moduleFile: Path,
    metadata: MutableGradleModuleMetadata,
  ) {
    // Gradle is hardcoded to publish modules to a Maven layout,
    // but we relocate all files to be in the same directory.
    // So, we can remove the relative paths.
    if (metadata.component.url?.startsWith("../../") == true) {
      metadata.component.url = metadata.component.url?.substringAfterLast("/")
    }

    metadata.variants.forEach { variant ->
      variant.files.forEach { file ->
        if (file.url.startsWith("../../")) {
          file.url = file.url.substringAfterLast("/")
        }
      }

      variant.availableAt?.let { aa ->
        if (aa.url.startsWith("../../")) {
          aa.url = aa.url.substringAfterLast("/")
        }
      }
    }

    metadata.saveTo(moduleFile)
  }

  private fun createIvyModuleFile(
    moduleFile: Path,
    metadata: GradleModuleMetadata,
  ) {
    // Create dummy Ivy file (otherwise Gradle can't find the Module Metadata file).
    // Only one Ivy file is required, pointing to the root module.
    // (i.e. KMP libraries have multiple modules, but only one root module.)
    val rootModuleName = metadata.component.module + "-" + metadata.component.version
    if (moduleFile.nameWithoutExtension == rootModuleName) {
      moduleFile
        .resolveSibling("$rootModuleName.ivy.xml")
        .writeText(
          // language=xml
          """
          |<?xml version="1.0"?>
          |<ivy-module version="2.0"
          |            xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          |            xsi:noNamespaceSchemaLocation="https://ant.apache.org/ivy/schemas/ivy.xsd">
          |    <!-- do_not_remove: published-with-gradle-metadata -->
          |    <info organisation="${metadata.component.group}" module="${metadata.component.module}" revision="${metadata.component.version}" />
          |</ivy-module>
          |""".trimMargin()
        )
    }
  }

  private fun createModuleChecksums(
    moduleFile: Path,
  ) {
    setOf(
      "256",
      "512",
    ).forEach {
      val checksum = moduleFile.computeChecksum("SHA-$it")
      moduleFile.resolveSibling(moduleFile.name + ".sha$it").writeText(checksum)
    }
  }

  companion object
}
