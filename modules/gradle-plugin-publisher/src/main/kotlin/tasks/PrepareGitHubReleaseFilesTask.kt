package dev.adamko.githubassetpublish.tasks

import java.io.File
import javax.inject.Inject
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.*
import org.gradle.api.tasks.PathSensitivity.RELATIVE
import org.gradle.process.ExecOperations

@CacheableTask // Performs simple file operations, but it uses JavaExec every time
abstract class PrepareGitHubReleaseFilesTask
@Inject
internal constructor(
  private val execOps: ExecOperations,
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
  abstract val stagingMavenRepo: DirectoryProperty

  @get:Classpath
  abstract val runtimeClasspath: ConfigurableFileCollection

  @TaskAction
  protected fun taskAction() {
    val arguments = mapOf(
      "sourceMavenRepositoryDir" to stagingMavenRepo.get().asFile.invariantSeparatorsPath,
      "destinationDir" to destinationDirectory.get().asFile.invariantSeparatorsPath,
    ).map { (key, value) -> "$key=$value" }

    execOps.javaexec { spec ->
      spec.mainClass.set("dev.adamko.githubassetpublish.lib.PrepareGitHubAssetsAction")
      spec.classpath(runtimeClasspath)
      spec.args(arguments)
    }
//    val repoDir = buildDirMavenDirectory.get().asFile.toPath()
//    val destinationDir = destinationDirectory.get().asFile.toPath()
//
//    logger.info("[$path] processing buildDirMavenRepo: $repoDir")
//
//    relocateFiles(
//      sourceDir = repoDir,
//      destinationDir = destinationDir,
//    )
//
//    updateRelocatedFiles(destinationDir)

    logger.info("[$path] outputDir:${destinationDirectory.get().asFile.invariantSeparatorsPath}")
  }

//  private fun buildTestHandle(
//    configure: JavaExecSpec.() -> Unit = {},
//  ): ExecResult {
//    return execOps.javaexec {
//      mainClass = "dev.adamko.githubassetpublish.lib.PrepareGitHubAssetsAction"
//      classpath(createClasspath())
//      configure()
//    }
////    return execOps.exec {
////      args(APP_FQN)
////      executable = currentJavaExecutable
////      environment("CLASSPATH", runtimeClasspath.asPath)
////      configure()
////    }
//  }

  private fun createClasspath(): List<String> {

//    println("System.getenv(\"CLASSPATH\") : ${System.getenv("CLASSPATH")}")
//    println("System.getProperty(\"java.class.path\") : ${System.getProperty("java.class.path")}")
//    println("runtimeClasspath.asPath : ${runtimeClasspath.asPath}")

    return listOfNotNull(
      System.getenv("CLASSPATH"),
      System.getProperty("java.class.path"),
      runtimeClasspath.asPath,
    )
      .flatMap { it.split(File.pathSeparator) }
      .mapNotNull { it.trim().ifBlank { null } }
  }

  companion object {
//    /** The current FQN of the test class. Used to launch [main] as a Java application. */
//    private val APP_FQN: String = PrepareGitHubReleaseFilesTask::class.qualifiedName!!
//
//    @JvmStatic
//    fun main(args: Array<String>) {
//      fun getArg(name: String): String =
//        args.firstOrNull { it.startsWith("$name=") }
//          ?.substringAfter("$name=")
//          ?: error("missing required argument '$name'")
//
//      val sourceMavenRepository = Path(getArg("sourceMavenRepository"))
//      val destinationDir = Path(getArg("destinationDir"))
//
//      val action = PrepareGitHubAssetsAction(
//        sourceMavenRepository = sourceMavenRepository,
//        destinationDir = destinationDir,
//      )
//
//      action.run()
//    }
//
//    /** The current Java executable. Used to launch [main]. */
//    private val currentJavaExecutable: String by lazy {
//      ProcessHandle.current().info().command().orElseThrow()
//    }
  }
}
