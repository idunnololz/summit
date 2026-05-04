plugins {
  id("com.android.library")
}

android {
  namespace = "io.noties.markwon.linkify"
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
  implementation("org.nibor.autolink:autolink:0.12.0")
  implementation(libs.commonmark)
  implementation(libs.core)
}
