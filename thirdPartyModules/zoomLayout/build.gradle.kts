plugins {
  id("com.android.library")
  id("kotlin-android")
}

android {
  namespace = "com.otaliastudios.zoom"
  compileSdk = 35

  defaultConfig {
    minSdk = 16
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
  implementation(libs.annotation)
}
