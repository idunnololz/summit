plugins {
  id("com.android.library")
}

android {
  namespace = "io.noties.markwon"
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
  implementation(libs.annotation)
  implementation(libs.appcompat)
  implementation(libs.commonmark)
  implementation(libs.core)
}
