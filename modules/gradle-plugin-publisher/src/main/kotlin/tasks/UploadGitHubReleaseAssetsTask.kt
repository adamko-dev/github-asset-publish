package dev.adamko.githubassetpublish.tasks

import dev.adamko.githubassetpublish.model.PublicationSpec
import java.nio.file.Path
import javax.inject.Inject
import kotlin.io.path.createDirectories
import kotlin.io.path.deleteRecursively
import kotlin.io.path.invariantSeparatorsPathString
import org.gradle.api.DefaultTask
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.api.tasks.PathSensitivity.RELATIVE
import org.gradle.process.ExecOperations
import org.gradle.work.DisableCachingByDefault

@DisableCachingByDefault(because = "Performs remote operations")
abstract class UploadGitHubReleaseAssetsTask
@Inject
internal constructor(
  private val execOps: ExecOperations,
) : DefaultTask() {

  @get:Input
  abstract val githubRepo: Property<String>

  /**
   * Output of the Maven Publish task.
   */
  @get:InputFiles
  @get:PathSensitive(RELATIVE)
  abstract val stagingMavenRepo: DirectoryProperty

//  @get:Input
//  abstract val version: Property<String>

  @get:Input
  abstract val createNewReleaseIfMissing: Property<Boolean>

//  @get:InputDirectory
//  @get:PathSensitive(NONE)
//  abstract val releaseDir: DirectoryProperty

  @get:Classpath
  abstract val runtimeClasspath: ConfigurableFileCollection

  @get:Nested
  abstract val publications: NamedDomainObjectContainer<PublicationSpec>

  @get:Internal
  abstract val workDir: DirectoryProperty

  @TaskAction
  protected fun taskAction() {
    preflightChecks()

    prepareWorkDir()

//    val releaseDir = releaseDir.get().asFile.toPath()
//    val stagingMavenRepo: Path = stagingMavenRepo.get().asFile.toPath()

    publications
      .filter { it.enabled.get() }
      .forEach { publication ->
        val preparedFilesDir = prepare(publication)
//        upload(publication, preparedFilesDir)
        upload()
      }
  }

  private fun preflightChecks() {
    // check if publication `group` can be mapped to `github-org/github-repo-name`
    // check if gh cli is installed
    // check if gh cli is authenticated
    // check if gh cli has access to GitHub repo
  }

  private fun prepareWorkDir() {
    workDir.get().asFile.toPath().apply {
      deleteRecursively()
      createDirectories()
    }
  }

  private fun prepare(publication: PublicationSpec): Path {
    val stagingMavenRepo: Path = stagingMavenRepo.get().asFile.toPath()
    val gmm = stagingMavenRepo.resolve(publication.pathToStagedMetadataModuleFile.get())

    val workDir = workDir.get().asFile.toPath()
    val (publicationGroupId, publicationModuleId) = publication.coordinates.get().split(":")

    val destinationDirectory: Path = workDir.resolve("$publicationGroupId/$publicationModuleId")

    val arguments = mapOf(
//      "sourceMavenRepositoryDir" to stagingMavenRepo.get().asFile.invariantSeparatorsPath,
      "gradleModuleMetadataFile" to gmm.invariantSeparatorsPathString,
      "destinationDir" to destinationDirectory.invariantSeparatorsPathString,
    ).map { (key, value) -> "$key=$value" }

    execOps.javaexec { spec ->
      spec.mainClass.set("dev.adamko.githubassetpublish.lib.PrepareGitHubAssetsAction")
      spec.classpath(runtimeClasspath)
      spec.args(arguments)
    }

    return workDir
  }

  private fun upload(
//    publication: PublicationSpec
  ) {
    println("skipping upload")
    return
//     execOps.javaexec { spec ->
//      spec.mainClass.set("dev.adamko.githubassetpublish.lib.Uploader")
//      spec.classpath(runtimeClasspath.files)
//      spec.args(
//        buildList {
//          add("githubRepo=${githubRepo.get()}")
////          add("releaseVersion=${version.get()}")
//          add("createNewReleaseIfMissing=${createNewReleaseIfMissing.get()}")
//          add("releaseDir=${releaseDir.get().asFile.absolutePath}")
//        }
//      )
//    }
  }

  companion object
}
