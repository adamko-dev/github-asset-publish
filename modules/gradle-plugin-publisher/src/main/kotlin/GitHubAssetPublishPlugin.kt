@file:Suppress("UnstableApiUsage")

package dev.adamko.githubassetpublish

//import dev.adamko.githubassetpublish.internal.PrepareReleaseDependencies
import dev.adamko.githubassetpublish.internal.UploadReleaseDependencies
import dev.adamko.githubassetpublish.tasks.UploadGitHubReleaseAssetsTask
import javax.inject.Inject
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.ProjectLayout
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin
import org.gradle.api.publish.maven.tasks.PublishToMavenRepository
import org.gradle.api.publish.plugins.PublishingPlugin
import org.gradle.api.tasks.Delete
import org.gradle.kotlin.dsl.*

abstract class GitHubAssetPublishPlugin
@Inject
internal constructor(
//  private val objects: ObjectFactory,
  private val providers: ProviderFactory,
  private val layout: ProjectLayout,
) : Plugin<Project> {

  override fun apply(project: Project) {

    val gapExtension = createExtension(project)

    project.pluginManager.apply(MavenPublishPlugin::class)

    val publishing = project.extensions.getByType<PublishingExtension>()

    configureTaskConventions(project, gapExtension)

//    val buildDirMavenRepo: MavenArtifactRepository =
    publishing.repositories.maven(gapExtension.stagingRepoDir) {
      name = gapExtension.stagingRepoName
    }

//    fun buildDirMavenDirectoryProvider(): Provider<Directory> =
//      objects.directoryProperty()
//        .fileProvider(providers.provider { buildDirMavenRepo }.map { it.url.toPath().toFile() })

    val cleanBuildDirMavenRepoDir by project.tasks.registering(Delete::class) {
//      val buildDirMavenRepoDir = gapExtension.stagingDir
      delete(gapExtension.stagingRepoDir)
    }

    val buildDirPublishTasks =
      project.tasks
      .withType<PublishToMavenRepository>()
      .matching{ task ->
        task.name.endsWith("PublicationToGitHubAssetPublishStagingRepository")

//        // need to determine the repo lazily because the repo isn't set immediately
//        val repoIsGapStaging = providers.provider { task.repository?.name == gapExtension.stagingRepoName }.orElse(false)
//        task.inputs.property("repoIsGapStaging", repoIsGapStaging)

      }


    buildDirPublishTasks.configureEach { task ->
      task.dependsOn(cleanBuildDirMavenRepoDir)
    }

    publishing.publications.withType<MavenPublication>().all { publication ->
      gapExtension.publications.create(publication.name) { spec ->
        spec.coordinates.convention(providers.provider {
          publication.run { "$groupId:$artifactId:$version" }
        })
      }
    }

//    val prepareGitHubReleaseFiles by project.tasks.registering(PrepareGitHubReleaseFilesTask::class) {
//      group = PublishingPlugin.PUBLISH_TASK_GROUP
//      dependsOn(buildDirPublishTasks)
//      stagingMavenRepo.convention(gapExtension.stagingDir)
//      val destinationDir = layout.buildDirectory.dir("github-release-files")
//      destinationDirectory.convention(destinationDir)
//    }

    val uploadGitHubReleaseAssets by project.tasks.registering(UploadGitHubReleaseAssetsTask::class) {
      group = PublishingPlugin.PUBLISH_TASK_GROUP

      dependsOn(buildDirPublishTasks)
      stagingMavenRepo.convention(gapExtension.stagingRepoDir)
//      releaseDir.convention(prepareGitHubReleaseFiles.flatMap { it.destinationDirectory })
    }
  }

  private fun createExtension(project: Project): GitHubAssetPublishExtension {
    return project.extensions.create<GitHubAssetPublishExtension>("gitHubAssetPublish").apply {

      gapBuildDir.convention(project.layout.buildDirectory.dir("github-asset-publish"))
      stagingRepoDir.convention(gapBuildDir.dir("staging-repo"))
//      assertsDir.convention(gapBuildDir.dir("assets"))
    }
  }

  private fun configureTaskConventions(
    project: Project,
    gapExtension: GitHubAssetPublishExtension,
  ) {
//    val prepareReleaseDependencies = PrepareReleaseDependencies(project)

//    val publishing = project.extensions.getByType<PublishingExtension>()

//    project.tasks.withType<PrepareGitHubReleaseFilesTask>().configureEach { task ->
//      task.runtimeClasspath.from(prepareReleaseDependencies.resolver)
//    }

    val uploadReleaseDependencies = UploadReleaseDependencies(project)

    project.tasks.withType<UploadGitHubReleaseAssetsTask>().configureEach { task ->
//      task.githubRepo.convention(providers.provider {
//        project.group.toString() + "/" + project.name
//      })
//      task.version.convention(providers.provider { project.version.toString() })
      task.createNewReleaseIfMissing.convention(true)
      task.runtimeClasspath.from(uploadReleaseDependencies.resolver)

      task.publications.addAllLater(providers.provider {
        gapExtension.publications
      })

      task.workDir.convention(layout.dir(providers.provider { task.temporaryDir }))
//      publishing.publications.withType<MavenPublication>().all { publication ->
////        publication
//      }
    }
  }
}
