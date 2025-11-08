@file:OptIn(ExperimentalSerializationApi::class)

package dev.adamko.githubassetpublish.lib.internal.model

import dev.adamko.githubassetpublish.lib.internal.model.GradleModuleMetadata.AttributeValue
import dev.adamko.githubassetpublish.lib.internal.model.GradleModuleMetadata.Companion.json
import java.nio.file.Path
import kotlin.io.path.inputStream
import kotlin.io.path.outputStream
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream

@Serializable
data class MutableGradleModuleMetadata(
  @EncodeDefault
  override val formatVersion: String = "1.1",
  override var component: Component,
  override var createdBy: CreatedBy,
  @EncodeDefault
  override var variants: MutableList<Variant> = mutableListOf(),
) : GradleModuleMetadata {

  @Serializable
  data class Component(
    override var group: String,
    override var module: String,
    override var version: String,
    override var url: String? = null,
    override var attributes: MutableMap<String, AttributeValue> = mutableMapOf(),
  ) : GradleModuleMetadata.Component

  @Serializable
  data class CreatedBy(
    override var gradle: Gradle? = null
  ) : GradleModuleMetadata.CreatedBy {
    @Serializable
    data class Gradle(
      override var version: String,
      override var buildId: String? = null,
    ) : GradleModuleMetadata.CreatedBy.Gradle
  }

  @Serializable
  data class Variant(
    override var name: String,
    override var attributes: MutableMap<String, AttributeValue> = mutableMapOf(),
    @SerialName("available-at")
    override var availableAt: AvailableAt? = null,
    override var dependencies: MutableList<Dependency> = mutableListOf(),
    override var dependencyConstraints: MutableList<DependencyConstraint> = mutableListOf(),
    override var files: MutableList<VariantFile> = mutableListOf(),
    override var capabilities: MutableList<Capability> = mutableListOf(),
  ) : GradleModuleMetadata.Variant

  @Serializable
  data class Dependency(
    override var group: String,
    override var module: String,
    override var version: VersionConstraint? = null,
    override var excludes: MutableList<Exclude> = mutableListOf(),
    override var reason: String? = null,
    override var attributes: MutableMap<String, AttributeValue> = mutableMapOf(),
    override var requestedCapabilities: MutableList<Capability> = mutableListOf(),
    override var endorseStrictVersions: Boolean? = null,
    override var thirdPartyCompatibility: ThirdPartyCompatibility? = null
  ) : GradleModuleMetadata.Dependency

  @Serializable
  data class DependencyConstraint(
    override var group: String,
    override var module: String,
    override var version: VersionConstraint? = null,
    override var reason: String? = null,
    override var attributes: MutableMap<String, AttributeValue> = mutableMapOf(),
  ) : GradleModuleMetadata.DependencyConstraint

  @Serializable
  data class VersionConstraint(
    override var requires: String? = null,
    override var prefers: String? = null,
    override var strictly: String? = null,
    override var rejects: MutableList<String> = mutableListOf(),
  ) : GradleModuleMetadata.VersionConstraint

  @Serializable
  data class Exclude(
    override var group: String,
    override var module: String,
  ) : GradleModuleMetadata.Exclude

  @Serializable
  data class Capability(
    override var group: String,
    override var name: String,
    override var version: String,
  ) : GradleModuleMetadata.Capability

  @Serializable
  data class VariantFile(
    override var name: String,
    override var url: String,
    override var size: Int,
    override var sha512: String,
    override var sha256: String,
    override var sha1: String,
    override var md5: String,
  ) : GradleModuleMetadata.VariantFile

  @Serializable
  data class AvailableAt(
    override var url: String,
    override var group: String,
    override var module: String,
    override var version: String,
  ) : GradleModuleMetadata.AvailableAt

  @Serializable
  data class ThirdPartyCompatibility(
    override var artifactSelector: ArtifactSelector? = null
  ) : GradleModuleMetadata.ThirdPartyCompatibility

  @Serializable
  data class ArtifactSelector(
    override var name: String? = null,
    override var type: String? = null,
    override var extension: String? = null,
    override var classifier: String? = null,
  ) : GradleModuleMetadata.ArtifactSelector

  companion object {
    fun loadFrom(file: Path): MutableGradleModuleMetadata {
      file.inputStream().use { source ->
        return json.decodeFromStream(serializer(), source)
      }
    }

    fun MutableGradleModuleMetadata.saveTo(file: Path) {
      file.outputStream().use { sink ->
        json.encodeToStream(serializer(), this, sink)
      }
    }
  }
}
