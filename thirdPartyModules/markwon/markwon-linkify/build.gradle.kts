plugins {
  id("com.android.library")
  id("org.jetbrains.kotlin.android")
}

android {
  namespace = "io.noties.markwon.linkify"
  compileSdk = 35

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
  implementation("org.nibor.autolink:autolink:0.12.0")
  implementation(libs.commonmark)
  implementation(libs.core)
}
