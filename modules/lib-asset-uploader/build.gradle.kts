plugins {
  buildsrc.`kotlin-lib`
}

dependencies {
  implementation(projects.modules.libGmm)
  implementation(projects.modules.libAssetUploaderApi)

  implementation(libs.kotlinx.serialization.json)
}
