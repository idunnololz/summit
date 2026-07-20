plugins {
  id("com.android.library")
  id("org.jetbrains.kotlin.plugin.parcelize")
  alias(libs.plugins.kotlin.plugin.serialization)
}

android {
  compileSdk = 36
  namespace = "com.idunnololz.summit.api"

  defaultConfig {
    minSdk = 23

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
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21

    isCoreLibraryDesugaringEnabled = true
  }
  kotlin {
    jvmToolchain(21)
  }
}

dependencies {
  implementation(libs.core.ktx)
  implementation(libs.gson)
  implementation(libs.retrofit2.retrofit)
  implementation(libs.kotlinx.serialization.json)

  coreLibraryDesugaring(libs.desugar.jdk.libs)
}
