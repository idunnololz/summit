plugins {
  id("com.android.library")
  id("org.jetbrains.kotlin.android")
  id("org.jetbrains.kotlin.plugin.parcelize")
  alias(libs.plugins.kotlin.plugin.serialization)
}

android {
  compileSdk = 36
  namespace = "com.idunnololz.summit.api"

  defaultConfig {
    minSdk = 21

    consumerProguardFiles("consumer-proguard-rules.txt")
  }

  buildTypes {
    release {
      proguardFiles(
        getDefaultProguardFile("proguard-android-optimize.txt"),
        "consumer-proguard-rules.txt",
      )
    }
  }
  buildFeatures {
    buildConfig = true
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
  implementation(libs.core.ktx)
  implementation(libs.gson)
  implementation(libs.retrofit2.retrofit)
  implementation(libs.kotlinx.serialization.json)
}
