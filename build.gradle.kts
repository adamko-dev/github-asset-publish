plugins {
  base
  idea
  `kotlin-dsl` apply false
}

tasks.updateDaemonJvm {
  languageVersion = JavaLanguageVersion.of(21)
}

idea {
  module {
    excludeDirs.addAll(
      files(
        ".idea/",
        ".kotlin/",
        "buildSrc/.kotlin/",
        "gradle/wrapper/",
      )
    )
  }
}
