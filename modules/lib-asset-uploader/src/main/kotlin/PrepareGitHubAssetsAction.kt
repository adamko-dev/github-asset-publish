package dev.adamko.githubassetpublish.lib

import dev.adamko.githubassetpublish.lib.internal.computeChecksum
import dev.adamko.githubassetpublish.lib.internal.model.GradleModuleMetadata
import dev.adamko.githubassetpublish.lib.internal.model.MutableGradleModuleMetadata
import dev.adamko.githubassetpublish.lib.internal.model.MutableGradleModuleMetadata.Companion.saveTo
import java.nio.file.Path
import kotlin.io.path.*

/**
 * Converts files published to a local directory in a Maven layout to files that can be attached to a GitHub Release.
 *
 * @param[gradleModuleMetadataFile] GMM file of the root module.
 * @param[destinationDir] The output directory for this task.
 * Will contain all files that should be attached as assets to a GitHub Release.
 */
class PrepareGitHubAssetsAction(
  private val gradleModuleMetadataFile: Path,
  private val destinationDir: Path,
  private val logger: Logger = Logger.Default,
) {

  fun run() {
    logger.debug(
      "Running PrepareGitHubAssetsAction." +
          "\n  gradleModuleMetadataFile: ${gradleModuleMetadataFile.invariantSeparatorsPathString}" +
          "\n  destinationDir: $destinationDir"
    )

    prepareDestinationDir(destinationDir)

    val rootGmm = GradleModuleMetadata.loadFrom(gradleModuleMetadataFile)

    val allModules = getAllGmms(gradleModuleMetadataFile)

    // TODO move check to _after_ files are relocated? Or check both before and after?
    checkModules(
      rootGmm = rootGmm,
      modules = allModules,
    )

    val relocatedModules =
      allModules.map { module ->
        relocateFiles(
          module = module,
          destinationDir = destinationDir,
        )
      }

    relocatedModules.forEach { module ->
      updateModuleMetadata(module)
      createModuleChecksums(module)
    }

    createIvyModuleFile(
      metadata = rootGmm,
      destinationDir = destinationDir,
    )

    logger.info("outputDir:${destinationDir.invariantSeparatorsPathString}")
  }

  private fun prepareDestinationDir(destinationDir: Path) {
    destinationDir.deleteRecursively()
    destinationDir.createDirectories()
  }

  private data class GradleModule(
    val gmmFile: Path,
    val gmm: MutableGradleModuleMetadata,
    /**
     * The files attached to the module's variants.
     *
     * All files must be siblings of [gmmFile].
     *
     * Use [Set] because some files might be available in multiple variants.
     */
    val files: Set<Path>,
  )

  private fun getAllGmms(
    rootGmmFile: Path
  ): List<GradleModule> {
    val queue = ArrayDeque<Path>()
    queue.add(rootGmmFile)

    val allGmms = mutableMapOf<Path, MutableGradleModuleMetadata>()

    while (queue.isNotEmpty()) {
      val gmmFile = queue.removeFirst()

      if (gmmFile in allGmms) continue

      val gmm = MutableGradleModuleMetadata.loadFrom(gmmFile)
      allGmms[gmmFile] = gmm

//      gmmFile.data.variants.forEach { variant ->
//        val availableAt = variant.availableAt
//        if (availableAt != null) {
//          queue.add(gmmFile.parent.resolve(availableAt.url))
//        }
//      }
    }

    return allGmms.map { (gmmFile, gmm) ->

      val files = gmm.variants.flatMap { variant ->
        variant.files.map { file ->
          gmmFile.resolveSibling(file.url)
        }
      }.toSet()

      GradleModule(
        gmmFile = gmmFile,
        gmm = gmm,
        files = files,
      )
    }
  }


  private fun checkModules(
    rootGmm: GradleModuleMetadata,
    modules: List<GradleModule>,
  ) {
    val errors = mutableListOf<String>()

    val invalidGroups = modules.filter { (_, moduleGmm, _) ->
      moduleGmm.component.group != rootGmm.component.group
    }
    if (invalidGroups.isNotEmpty()) {
      errors.add("The group of all variants must be '${rootGmm.component.group}', but found: $invalidGroups")
    }
    val invalidVersions = modules.filter { (_, moduleGmm, _) ->
      moduleGmm.component.version != rootGmm.component.version
    }
    if (invalidVersions.isNotEmpty()) {
      errors.add("The version of all variants must be '${rootGmm.component.version}', but found: $invalidVersions")
    }

    modules.forEach { (gmmFile, gmm, files) ->
      val invalidFiles = files.filter { f -> gmmFile.parent != f.parent }
      if (invalidFiles.isNotEmpty()) {
        errors.add("${gmm.component.module} has files in invalid location: ${invalidFiles.map { it.invariantSeparatorsPathString }}")
      }
    }

    if (errors.isNotEmpty()) {
      error(errors.joinToString("\n"))
    }
  }

  /**
   * Relocate publishable files on disk to be in a single directory, [destinationDir],
   * without nesting.
   */
  private fun relocateFiles(
    module: GradleModule,
    destinationDir: Path,
  ): GradleModule {
    val relocatedGmmFile = module.gmmFile.copyTo(destinationDir.resolve(module.gmmFile.name))

    val relocatedArtifacts = module.files.map { src ->
      logger.info("relocating file: $src to ${destinationDir.resolve(src.name)}")
      src.copyTo(destinationDir.resolve(src.name))
    }.toSet()

    return GradleModule(
      gmmFile = relocatedGmmFile,
      gmm = MutableGradleModuleMetadata.loadFrom(relocatedGmmFile),
      files = relocatedArtifacts,
    )
  }

//  private fun updateRelocatedModule(
//    module: GradleModule,
////    destinationDir: Path,
//  ) {
//    updateModuleMetadata(module)
////    createModuleChecksums(moduleFile)
////    destinationDir.findModuleMetadataFiles().forEach { (moduleFile, metadata) ->
//////      createIvyModuleFile(moduleFile, metadata)
////    }
//  }

//  private fun Path.findModuleMetadataFiles(): Sequence<Pair<Path, MutableGradleModuleMetadata>> =
//    walk()
//      .filter { it.isRegularFile() && it.extension == "module" }
//      .mapNotNull { moduleFile ->
//        try {
//          val metadata = MutableGradleModuleMetadata.loadFrom(moduleFile)
//          moduleFile to metadata
//        } catch (ex: Exception) {
//          logger.warn("failed to load moduleFile ${moduleFile.invariantSeparatorsPathString}", ex)
//          null
//        }
//      }

  /**
   * Gradle is hardcoded to publish modules to a Maven layout,
   * but we relocate all files to be in the same directory.
   * So, we must update the GMM to remove the relative paths
   * of artifacts attached to the module.
   */
  private fun updateModuleMetadata(
    module: GradleModule,
//    metadata: MutableGradleModuleMetadata,
  ) {
    if (module.gmm.component.url?.startsWith("../../") == true) {
      module.gmm.component.url = module.gmm.component.url?.substringAfterLast("/")
    }

    module.gmm.variants.forEach { variant ->
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

    module.gmm.saveTo(module.gmmFile)
  }

  /**
   * Create a dummy Ivy file (otherwise Gradle can't find the Module Metadata file).
   * Workaround for https://github.com/gradle/gradle/issues/33674
   *
   * Only one Ivy file is required, pointing to the root module.
   * (i.e. KMP libraries have multiple secondary variant modules, but only one root module.)
   */
  private fun createIvyModuleFile(
    metadata: GradleModuleMetadata,
    destinationDir: Path,
  ) {
    val rootModuleName = metadata.component.module + "-" + metadata.component.version
    destinationDir
      .resolve("$rootModuleName.ivy.xml")
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

  private fun createModuleChecksums(module: GradleModule) {
    setOf(
      "256",
      "512",
    ).forEach {
      val checksum = module.gmmFile.computeChecksum("SHA-$it")
      module.gmmFile.resolveSibling(module.gmmFile.name + ".sha$it").writeText(checksum)
    }
  }

  companion object {
    @JvmStatic
    fun main(args: Array<String>) {
      fun getArg(name: String): String =
        args.firstOrNull { it.startsWith("$name=") }
          ?.substringAfter("$name=")
          ?: error("missing required argument '$name'")

      val gradleModuleMetadataFile = Path(getArg("gradleModuleMetadataFile"))
      val destinationDir = Path(getArg("destinationDir"))

      val action = PrepareGitHubAssetsAction(
        gradleModuleMetadataFile = gradleModuleMetadataFile,
        destinationDir = destinationDir,
      )

      action.run()
    }
  }
}
