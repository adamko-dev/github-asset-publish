@file:Suppress("UnstableApiUsage")

package buildsrc

import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
  id("buildsrc.base")
  kotlin("jvm")
  id("dev.adamko.dev-publish")
}

kotlin {
  jvmToolchain(21)

  compilerOptions {
    jvmTarget = JvmTarget.JVM_17

    optIn.add("kotlin.io.path.ExperimentalPathApi")
    optIn.add("kotlin.time.ExperimentalTime")
    optIn.add("kotlin.ExperimentalStdlibApi")

    freeCompilerArgs.addAll(
      "-Xconsistent-data-class-copy-visibility",
      "-Xcontext-parameters",
      "-Xwhen-guards",
      "-Xnon-local-break-continue",
      "-Xmulti-dollar-interpolation",
    )

    freeCompilerArgs.add(jvmTarget.map { "-Xjdk-release=${it.target}" })
  }
}

tasks.withType<JavaCompile>().configureEach {
  targetCompatibility = kotlin.compilerOptions.jvmTarget.get().target
}

testing.suites.withType<JvmTestSuite>().configureEach {
  useJUnitJupiter("6.0.1")
}
