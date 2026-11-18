plugins {
  id("com.android.library")
}

android {
  namespace = "com.idunnololz.animatedavif"
  compileSdk = 37

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
  implementation(libs.coil)
  implementation(libs.avif)
  implementation(libs.coil.gif)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.lifecycle.runtime)

  testImplementation("junit:junit:4.13.2")
}
