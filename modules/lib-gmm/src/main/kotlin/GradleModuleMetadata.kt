@file:OptIn(ExperimentalSerializationApi::class)

package dev.adamko.githubassetpublish.lib.internal.model

import java.nio.file.Path
import kotlin.io.path.inputStream
import kotlinx.serialization.*
import kotlinx.serialization.json.*

/**
 * Represents the root of the Gradle Module Metadata file (formatVersion 1.1).
 * Describes the module's content including its component, the producer, and its variants.
 */
@Serializable
sealed interface GradleModuleMetadata {
  /** Describes the identity of the component in the module. */
  val component: Component
  /** Describes the producer of this metadata file. */
  val createdBy: CreatedBy?
  /**
   * A list of variants describing the module. Each variant speaks to a specific configuration or platform.
   *
   * Must contain an array with zero or more elements.
   */
  val variants: List<Variant>
  /** The version of the metadata format. Must be "1.1". */
  @EncodeDefault
  val formatVersion: String

  /**
   * Represents the identity of a component contained in the module.
   */
  sealed interface Component {
    /** The group of this component (e.g., `com.example`). */
    val group: String
    /** The name of this component (e.g., `my-library`). */
    val module: String
    /** The version of this component. */
    val version: String
    /** Optional. URL where the metadata for the component may be found. */
    val url: String?
    /** Optional. Attributes of the component as key-value pairs. */
    val attributes: Map<String, AttributeValue>
  }

  /**
   * Represents information about the producer of this metadata file.
   */
  sealed interface CreatedBy {
    /** Describes the Gradle instance that created this metadata. */
    val gradle: Gradle?
    /**
     * Represents details of the Gradle instance that created the module.
     */
    sealed interface Gradle {
      /** The version of the Gradle instance. */
      val version: String
      /** Optional. A unique identifier of the Gradle build. */
      val buildId: String?
    }
  }

  /**
   * Represents a specific variant of the module.
   */
  sealed interface Variant {
    /** The name of the variant. Must be unique across all variants. */
    val name: String
    /** Optional. Attributes for the variant as key-value pairs. */
    val attributes: Map<String, AttributeValue>
    /** Optional. URL and other information about where metadata/files of this variant are available. */
    @SerialName("available-at")
    val availableAt: AvailableAt?
    /** Optional. List of dependencies specific to this variant. */
    val dependencies: List<Dependency>
    /** Optional. Constraints on dependencies for this variant. */
    val dependencyConstraints: List<DependencyConstraint>
    /** Optional. A list of files included for this variant. */
    val files: List<VariantFile>
    /** Optional. List of specific capabilities declared by this variant. */
    val capabilities: List<Capability>
  }

  /**
   * Represents a dependency of a variant.
   */
  sealed interface Dependency {
    /** The group of the dependency. */
    val group: String
    /** The name of the dependency module. */
    val module: String
    /** Optional. The version constraints for the dependency. */
    val version: VersionConstraint?
    /** Optional. Defines exclusions that apply to this dependency. */
    val excludes: List<Exclude>
    /** Optional. Explanation for this dependency's usage. */
    val reason: String?
    /** Optional. Attributes applied to override consumer attributes during resolution. */
    val attributes: Map<String, AttributeValue>
    /** Optional. Declares the capabilities required for dependency selection. */
    val requestedCapabilities: List<Capability>
    /** Optional. If true, treats strict versions as part of the defining variant. */
    val endorseStrictVersions: Boolean?
    /** Optional. Used for modules without Gradle module metadata. */
    val thirdPartyCompatibility: ThirdPartyCompatibility?
  }

  /**
   * Represents a dependency constraint in a variant.
   */
  sealed interface DependencyConstraint {
    /** The group of the dependency constraint. */
    val group: String
    /** The name of the dependency constraint module. */
    val module: String
    /** Optional. Version constraint for the dependency. */
    val version: VersionConstraint?
    /** Optional. Explanation for the constraint. */
    val reason: String?
    /** Optional. Attributes that override consumer attributes during resolution. */
    val attributes: Map<String, AttributeValue>
  }

  /**
   * Represents version constraints for a dependency or dependency constraint.
   */
  sealed interface VersionConstraint {
    /** Optional. The required version for this dependency. */
    val requires: String?
    /** Optional. The preferred version. */
    val prefers: String?
    /** Optional. A strictly enforced version. */
    val strictly: String?
    /** Optional. A list of rejected versions. */
    val rejects: List<String>
  }

  /**
   * Represents an exclusion rule to apply to a dependency.
   */
  sealed interface Exclude {
    /** The group to exclude; may be `*` for all groups. */
    val group: String
    /** The module to exclude; may be `*` for all modules. */
    val module: String
  }

  /**
   * Represents a capability declared by a variant.
   */
  sealed interface Capability {
    /** The group of the capability. */
    val group: String
    /** The name of the capability. */
    val name: String
    /** The version of the capability. */
    val version: String
  }

  /**
   * Represents a file included in a variant.
   */
  sealed interface VariantFile {
    /** The name of the file. Must be unique across all variants. */
    val name: String
    /** The relative URL where the file is located. */
    val url: String
    /** The size of the file in bytes. */
    val size: Int
    /** The SHA-512 checksum of the file content. */
    val sha512: String
    /** The SHA-256 checksum of the file content. */
    val sha256: String
    /** The SHA-1 checksum of the file content. */
    val sha1: String
    /** The MD5 checksum of the file content. */
    val md5: String
  }

  /**
   * Represents the location and metadata of a variant available elsewhere.
   */
  sealed interface AvailableAt {
    /** The relative URL to the metadata file describing the variant. */
    val url: String
    /** The group of the module. */
    val group: String
    /** The name of the module. */
    val module: String
    /** The version of the module. */
    val version: String
  }

  /**
   * Includes compatibility information for third-party modules.
   */
  sealed interface ThirdPartyCompatibility {
    /** Optional. Information selectors for specific artifacts of dependencies without Gradle metadata. */
    val artifactSelector: ArtifactSelector?
  }

  /**
   * A selector for specific artifacts in dependencies.
   */
  sealed interface ArtifactSelector {
    /** Optional. The name of the artifact. */
    val name: String?
    /** Optional. The type of the artifact (e.g., `jar`, `war`). */
    val type: String?
    /** Optional. The artifact's file extension. */
    val extension: String?
    /** Optional. The classifier for the artifact. */
    val classifier: String?
  }

  @Serializable(with = AttributeValueSerializer::class)
  sealed interface AttributeValue

  @Serializable
  @JvmInline
  value class StringAttribute(val value: String) : AttributeValue

  @Serializable
  @JvmInline
  value class BooleanAttribute(val value: Boolean) : AttributeValue

  @Serializable
  @JvmInline
  value class IntegerAttribute(val value: Int) : AttributeValue

  object AttributeValueSerializer : JsonContentPolymorphicSerializer<AttributeValue>(AttributeValue::class) {
    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<AttributeValue> {
      require(element is JsonPrimitive) { "Expected a JsonPrimitive, got $element" }

      if (element.isString) return StringAttribute.serializer()
      if (element.booleanOrNull != null) return BooleanAttribute.serializer()
      if (element.intOrNull != null) return IntegerAttribute.serializer()

      error("Unsupported attribute value type: $element")
    }
  }

  companion object {
    internal val json: Json =
      Json {
        prettyPrint = true
        prettyPrintIndent = "  "
      }

    fun loadFrom(file: Path): GradleModuleMetadata {
      file.inputStream().use { source ->
        return json.decodeFromStream(MutableGradleModuleMetadata.Companion.serializer(), source)
      }
    }
  }
}
