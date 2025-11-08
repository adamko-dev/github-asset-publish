package dev.adamko.githubassetpublish.model

import javax.inject.Inject
import org.gradle.api.Describable
import org.gradle.api.Named
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal

abstract class PublicationSpec
@Inject
internal constructor(
  private val name: String,
//  val coordinates: String,
) : Named, Describable {

//  @get:Input
//  abstract val artifactId: Property<String>
//
//  @get:Input
//  abstract val groupId: Property<String>
//
//  @get:Input
//  abstract val version: Property<String>

  @get:Input
  abstract val coordinates: Property<String>

  @get:Input
  abstract val pathToStagedMetadataModuleFile: Property<String>

//  private val named: String = coordinates
//    .split(':')
//    .take(2)
//    .flatMap { it.split("""\W""".toRegex()) }
//    .joinToString(separator = "") { it.replaceFirstChar(Char::titlecase) }
//    .replaceFirstChar { it.lowercase() }

  @get:Input
  abstract val enabled: Property<Boolean>

  @Internal
  override fun getName(): String = name

  @Internal
  override fun getDisplayName(): String =
    "PublicationSpec(${coordinates.orNull}, ${pathToStagedMetadataModuleFile.orNull})"

  companion object {
    internal fun ObjectFactory.newPublicationSpec(
      name: String,
//      groupId: Provider<String>,
//      artifactId: Provider<String>,
//      version: Provider<String>,
    ): PublicationSpec =
      newInstance(PublicationSpec::class.java, name).apply {
        enabled.convention(true)
//        gav.convention(
//          groupId
//            .zip(artifactId) { groupId, artifactId -> "$groupId:$artifactId" }
//            .zip(version) { groupArtifact, version -> "$groupArtifact:$version" }
//        )
        pathToStagedMetadataModuleFile.convention(
          coordinates.map { gav ->
            val (group, artifact, version) = gav.split(":", limit = 3)
            val rewrittenGroup = group.replace(".", "/")
            "$rewrittenGroup/$artifact/$version/$artifact-$version.module"
          }
        )
      }
  }
}
