@file:Suppress("UnstableApiUsage")

package dev.adamko.githubassetpublish.internal

//import org.gradle.api.NamedDomainObjectProvider
//import org.gradle.api.Project
//import org.gradle.api.artifacts.DependencyScopeConfiguration
//import org.gradle.api.artifacts.ResolvableConfiguration
//import org.gradle.api.model.ObjectFactory
//
//internal class PrepareReleaseDependencies internal constructor(
//  project: Project,
//) {
//  private val objects: ObjectFactory = project.objects
//
//  private val dependenciesContainer: NamedDomainObjectProvider<DependencyScopeConfiguration> =
//    project.configurations.dependencyScope("githubAssetPublishClasspath") { c ->
//      c.description = "The classpath used to prepare GitHub Release Assets."
//      c.defaultDependencies { dependencies ->
//        dependencies.add(project.dependencies.create(BuildConstants.libGmmRewriterCoords))
//      }
//    }
//
//  val resolver: NamedDomainObjectProvider<ResolvableConfiguration> =
//    project.configurations.resolvable(dependenciesContainer.name + "Resolver") { c ->
//      c.extendsFrom(dependenciesContainer.get())
//      setJvmJarAttributes(objects, c.attributes)
//    }
//}
