plugins {
  id("com.android.library")
}

android {
  namespace = "com.otaliastudios.zoom"
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
}
