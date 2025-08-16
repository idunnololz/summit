plugins {
  id("com.android.library")
  id("org.jetbrains.kotlin.android")
}

android {
  namespace = "io.noties.markwon.simple.ext"
  compileSdk = 36

  defaultConfig {
    minSdk = 21
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }
  kotlin {
    jvmToolchain(17)
  }
}

dependencies {
  implementation(project(":thirdPartyModules:markwon:markwon-core"))
  implementation(libs.commonmark)
  implementation(libs.core)
}
