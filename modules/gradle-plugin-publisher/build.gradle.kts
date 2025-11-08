@file:Suppress("UnstableApiUsage")
@file:OptIn(ExperimentalPathApi::class)

import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.createDirectories
import kotlin.io.path.deleteRecursively
import kotlin.io.path.writeText

//import org.gradle.api.attributes.Usage.JAVA_RUNTIME
//import org.gradle.api.attributes.Usage.USAGE_ATTRIBUTE
//import org.gradle.kotlin.dsl.support.serviceOf

plugins {
  buildsrc.`kotlin-gradle-plugin`
}

gradlePlugin {
  plugins {
    register("GitHubAssetPublish") {
      id = "dev.adamko.github-asset-publisher"
      implementationClass = "dev.adamko.githubassetpublish.GitHubAssetPublishPlugin"
    }
  }
}

dependencies {
  implementation(projects.modules.libAssetUploaderApi)

//  devPublication(projects.modules.libGmmRewriter)
  devPublication(projects.modules.libAssetUploader)
  devPublication(projects.modules.libAssetUploaderApi)
  devPublication(projects.modules.libGmm)
}

testing {
  suites {
    withType<JvmTestSuite>().configureEach {
      targets.configureEach {
        testTask.configure {
          systemProperty("junit.jupiter.tempdir.cleanup.mode.default", "ON_SUCCESS")
        }
      }
    }

    val testIntegration by registering(JvmTestSuite::class) {
      dependencies {
        implementation(gradleTestKit())
        implementation("org.junit.jupiter:junit-jupiter:6.0.1")
        runtimeOnly("org.junit.platform:junit-platform-launcher")
      }

      targets.configureEach {
        testTask.configure {
          val devMavenRepo = devPublish.devMavenRepo
          dependsOn(tasks.updateDevRepo)
          jvmArgumentProviders.add {
            listOf(
              "-DdevMavenRepo=${devMavenRepo.get().asFile.invariantSeparatorsPath}"
            )
          }
        }
      }
    }
    tasks.check { dependsOn(testIntegration) }
  }
}


//val prepareGitHubReleaseDependencies by configurations.dependencyScope("prepareGitHubReleaseDependencies") {
//  defaultDependencies {
//    add(projects.modules.libGmmRewriter)
//    add(project.dependencies.gradleApi())
//    add(project.gradleKotlinDsl())
//    add(project.dependencies.create("org.slf4j:slf4j-simple:2.0.17"))
//  }
//}
//
//val prepareGitHubReleaseDependenciesResolver by configurations.resolvable(prepareGitHubReleaseDependencies.name + "Resolver") {
//  extendsFrom(prepareGitHubReleaseDependencies)
//  attributes {
//    attribute(USAGE_ATTRIBUTE, objects.named(JAVA_RUNTIME))
//  }
//}
//
//dependencies {
////  add("githubAssetPublishClasspath", projects.modules.githubAssetPublishLib)
////  "githubAssetPublishClasspath"("")
//}

//tasks.withType<PrepareGitHubReleaseFilesTask>().configureEach {
//  runtimeClasspath.from(prepareGitHubReleaseDependenciesResolver)
//  runtimeClasspath.from(fileTree("build/install"))
//}

//val buildDirMavenRepo =
//  publishing.repositories.maven(layout.buildDirectory.dir("build-dir-maven")) {
//    name = "BuildDir"
//  }

//fun buildDirMavenDirectoryProvider(): Provider<Directory> =
//  objects.directoryProperty()
//    .fileProvider(providers.provider { buildDirMavenRepo }.map { it.url.toPath().toFile() })
//
//val cleanBuildDirMavenRepoDir by project.tasks.registering(Delete::class) {
//  val buildDirMavenRepoDir = buildDirMavenDirectoryProvider()
//  delete(buildDirMavenRepoDir)
//}


//tasks.withType<PublishToMavenRepository>()
//  .matching { it.name.endsWith("ToBuildDirRepository") }
//  .configureEach {
//    val providers = serviceOf<ProviderFactory>()
//    val exec = serviceOf<ExecOperations>()
//    val fs = serviceOf<FileSystemOperations>()
//
//    dependsOn(cleanBuildDirMavenRepoDir)
//
//    val execClasspath = prepareGitHubReleaseDependenciesResolver.incoming.files
//    inputs.files(execClasspath)
//      .withPropertyName("execClasspath")
//      .withNormalizer(ClasspathNormalizer::class)
//
//    val repoDir = providers.provider { repository.url.toPath() }
//    val destinationDir = temporaryDir.toPath()
//
//    doLast {
////     val repoDir = repository.url.toPath()
////      println("repoDir ${repoDir.get().walk().toList()}")
//
//      fs.delete { delete(destinationDir) }
//      destinationDir.createDirectories()
//
//      val arguments: List<String> = mapOf(
//        "sourceMavenRepositoryDir" to repoDir.get().invariantSeparatorsPathString,
//        "destinationDir" to destinationDir.invariantSeparatorsPathString,
//      ).map { (key, value) -> "$key=$value" }
//
//      exec.javaexec {
//        mainClass = "dev.adamko.githubassetpublish.lib.PrepareGitHubAssetsAction"
//        classpath = execClasspath
//        args(arguments)
//      }
//    }
//  }


//gradlePlugin.plugins.configureEach plugin@{
//  val pluginName = this@plugin.name
//  val mavenPublications = publishing.publications.withType<MavenPublication>()
//
//  //println("components: ${components.names}")
//  val javaComponent = components["java"] as AdhocComponentWithVariants
//
//  //println("mavenPublications: ${mavenPublications.names}")
//  val pluginMarkerPublication =
//    providers.provider {
//      mavenPublications.named(this@plugin.name + "PluginMarkerMaven")
//    }.flatMap { it }
//
//
//  val generateMetadataTask: TaskProvider<GenerateModuleMetadata> =
//    tasks.register<GenerateModuleMetadata>("generateModuleMetadataForPlugin${pluginName}") {
//      description = "Generates the Gradle metadata file for Gradle Plugin '${pluginName}'."
//      group = PublishingPlugin.PUBLISH_TASK_GROUP
//      publication.set(pluginMarkerPublication)
//      publications.set(mavenPublications)
//      outputFile.set(temporaryDir.resolve("module.json"))
//    }
//
//
//  mavenPublications.matching { it.name == this@plugin.name + "PluginMarkerMaven" }
//    .configureEach {
//      from(javaComponent)
//    }
//}


val generateBuildConstants by tasks.registering {
  group = project.name

  val outputDir = temporaryDir.toPath()
  outputs.dir(outputDir).withPropertyName("outputDir")
  outputs.cacheIf { true }

//  val libGmmRewriterCoords = providers.provider {
//    projects.modules.libGmmRewriter.run { "$group:$name:$version" }
//  }
//  inputs.property("libGmmRewriterCoords", libGmmRewriterCoords)
  val libAssetUploaderCoords = providers.provider {
    projects.modules.libAssetUploader.run { "$group:$name:$version" }
  }
  inputs.property("libAssetUploaderCoords", libAssetUploaderCoords)

  val taskPath = providers.provider { path }
  inputs.property("taskPath", taskPath)

  doLast {
    outputDir.deleteRecursively()
    outputDir.resolve("BuildConstants.kt").apply {
      parent.createDirectories()
      writeText(
        """
        |// DO NOT EDIT - generated by ${taskPath.get()}
        |@file:Suppress("ConstPropertyName")
        |
        |package dev.adamko.githubassetpublish.internal
        |
        |internal object BuildConstants {
        |  const val libAssetUploaderCoords: String = "${libAssetUploaderCoords.get()}"
        |}
        |""".trimMargin()
//        |  const val libGmmRewriterCoords: String = "${libGmmRewriterCoords.get()}"
      )
    }
  }
}

kotlin.sourceSets.main {
  kotlin.srcDir(generateBuildConstants)
}
