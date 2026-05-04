plugins {
  id("com.android.library")
}

android {
  namespace = "io.noties.markwon.ext.tables"
  compileSdk = 36

  defaultConfig {
    minSdk = 23
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
  }
  kotlin {
    jvmToolchain(21)
  }
}

dependencies {
  implementation(project(":thirdPartyModules:markwon:markwon-core"))
  implementation(libs.annotation)
  implementation(libs.commonmark.table)
}
