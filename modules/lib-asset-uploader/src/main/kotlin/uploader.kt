@file:JvmName("Uploader")

package dev.adamko.githubassetpublish.lib

import java.nio.file.Path
import java.util.concurrent.TimeUnit.MINUTES
import kotlin.io.path.Path
import kotlin.io.path.invariantSeparatorsPathString
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

fun main(args: Array<String>) {
  /*

  Requires `gh` cli is installed and authenticated.

  1. Get the version of the release.
  2. Check if the GitHub repo has a compatible release.
  3. Check if the release already has files.
     - FAIL: if not snapshot version and files already exist
  4. Upload assets to the release.
  5. If snapshot version, delete old files.

   */


  fun getArg(name: String): String =
    args.firstOrNull { it.startsWith("$name=") }
      ?.substringAfter("$name=")
      ?: error("missing required argument '$name'")


  /** `OWNER/REPO` */
  val githubRepo = getArg("githubRepo")
  val releaseVersion = getArg("releaseVersion")
  val releaseDir = getArg("releaseDir").let(::Path)
  val createNewReleaseIfMissing = getArg("createNewReleaseIfMissing").toBoolean()

  val release = FilesToUploadWithMetadata(
    repo = githubRepo,
    version = releaseVersion,
    releaseDir = releaseDir,
  )

  upload(
    release = release,
    createNewReleaseIfMissing = createNewReleaseIfMissing,
  )
}


private data class FilesToUploadWithMetadata(
  /** Repository in the form of "owner/repo" */
  val repo: String,
  /** Version of the release */
  val version: String,
//  /** Whether this is a snapshot version */
//  val isSnapshot: Boolean,
  /** Directory containing all files to upload. */
  val releaseDir: Path,
) {
//  val isSnapshot: Boolean
//    get() = version.endsWith("-SNAPSHOT")
}

private fun upload(
  release: FilesToUploadWithMetadata,
  createNewReleaseIfMissing: Boolean,
) {
  val repo = release.repo
  val version = release.version
//  val isSnapshot = release.version.endsWith("-SNAPSHOT")
//  val filesToUpload = release.filesToUpload

  val gh = GitHub(
    repo = repo,
    workDir = release.releaseDir,
  )

  val ghVersion = gh.version() ?: error("gh not installed")
  println("using gh version: $ghVersion")

  println("Starting upload process for version: $version, repo: $repo")

  // Check if the release exists
  if (gh.releaseView(tag = version) == null) {
    if (createNewReleaseIfMissing) {
      println("Creating new release $version...")
      val result = gh.releaseCreate(tag = version)
      println(result)
    } else {
      error("Release $version does not exist and createNewReleaseIfMissing is false.")
    }
  }


  // Check if the release already has files
  val existingFiles: List<String> =
    gh.releaseListAssets(tag = version)
      .ifBlank { "[]" }
      .let { Json.decodeFromString(ListSerializer(String.serializer()), it) }


  if (existingFiles.isNotEmpty()) {
    error("Release $version already has files. Exiting.")
  }


  // Upload new files
  println("Uploading files to release $version...")
  val filesToUpload = release.releaseDir.listDirectoryEntries().map { it.name }

  gh.releaseUpload(tag = version, files = filesToUpload)

  println("Upload process completed for version: $version")
}


private abstract class CliTool(
  private val workDir: Path,
) {

  protected fun runCommandOrNull(
    cmd: String,
    logOutput: Boolean = true,
  ): String? {
    return try {
      runCommand(cmd, logOutput)
    } catch (_: ProcessException) {
      null
    }
  }

  protected fun runCommand(
    cmd: String,
    logOutput: Boolean = true,
  ): String {

    val args = parseSpaceSeparatedArgs(cmd)

    val process = ProcessBuilder(args).apply {
      redirectOutput(ProcessBuilder.Redirect.PIPE)
      redirectInput(ProcessBuilder.Redirect.PIPE)
      redirectErrorStream(true)
      directory(workDir.toFile())
    }.start()

    val processOutput = process.inputStream
      .bufferedReader()
      .lineSequence()
      .onEach { if (logOutput) println("\t$it") }
      .joinToString("\n")
      .trim()

    process.waitFor(10, MINUTES)

    val exitCode = process.exitValue()

    return if (exitCode == 0) {
      processOutput
    } else {
      throw ProcessException(
        cmd = args.joinToString(" "),
        exitCode = exitCode,
        processOutput = processOutput,
      )
    }
  }

  private class ProcessException(
    cmd: String,
    exitCode: Int,
    processOutput: String,
  ) : Exception(
    buildString {
      appendLine("Command '$cmd' failed with exit code $exitCode")
      appendLine(processOutput.prependIndent())
    }
  )

  companion object {
    private fun parseSpaceSeparatedArgs(argsString: String): List<String> {
      val parsedArgs = mutableListOf<String>()
      var inQuotes = false
      var currentCharSequence = StringBuilder()
      fun saveArg(wasInQuotes: Boolean) {
        if (wasInQuotes || currentCharSequence.isNotBlank()) {
          parsedArgs.add(currentCharSequence.toString())
          currentCharSequence = StringBuilder()
        }
      }
      argsString.forEach { char ->
        if (char == '"') {
          inQuotes = !inQuotes
          // Save value which was in quotes.
          if (!inQuotes) {
            saveArg(true)
          }
        } else if (char.isWhitespace() && !inQuotes) {
          // Space is separator
          saveArg(false)
        } else {
          currentCharSequence.append(char)
        }
      }
      if (inQuotes) {
        error("No close-quote was found in $currentCharSequence.")
      }
      saveArg(false)
      return parsedArgs
    }
  }
}

/** GitHub commands */
private class GitHub(
  private val repo: String,
  workDir: Path,
) : CliTool(workDir = workDir) {


//  fun releaseList(tag: String): String? =
//    runCommandOrNull("gh release view $tag --repo $repo")

  fun releaseCreate(tag: String): String =
    runCommand(
      buildString {
        appendLine("gh release create $tag")
//        appendLine("--verify-tag")
        appendLine("--draft")
        appendLine("--title $tag")
        appendLine("--repo $repo")
      }
//        append(" --prerelease ")
//        append(" --fail-on-no-commits ")
    )

  fun releaseUpload(tag: String, files: Iterable<String>): String =
    runCommand(
      buildString {
        appendLine("gh release upload $tag")
        appendLine("--repo $repo")
        appendLine(files.joinToString(" "))
      }
    )

  fun releaseView(tag: String): String? =
    runCommandOrNull("gh release view $tag --repo $repo")

  fun releaseListAssets(tag: String): String =
    runCommand("gh release view $tag --repo $repo --json assets --jq .assets[].name")
//    runCommand("gh release view $tag --repo $repo --json assets")

  fun releaseDeleteAsset(tag: String, assetName: String): String? =
    runCommandOrNull("gh release delete-asset $tag $assetName --repo $repo --yes")

  fun releaseDownload(
    tag: String,
    pattern: String,
    destination: Path? = null,
  ): String? =
    runCommandOrNull(
      buildString {
        appendLine("gh release download $tag")
        appendLine("--repo $repo")
        if (destination != null) {
          appendLine("--dest ${destination.invariantSeparatorsPathString}")
        }
        appendLine("--pattern $pattern")
      }
    )

  fun version(): String? =
    runCommandOrNull("gh version", logOutput = false)
}
