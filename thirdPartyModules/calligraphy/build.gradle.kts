plugins {
  id("com.android.library")
}

android {
  compileSdk = 36
  namespace = "io.github.inflationx.calligraphy3"

  defaultConfig {
    minSdk = 23

    consumerProguardFiles("consumer-proguard-rules.txt")
  }

  buildTypes {
    release {
      isMinifyEnabled = false
      proguardFiles(
        getDefaultProguardFile("proguard-android-optimize.txt"),
        "consumer-proguard-rules.txt",
      )
    }
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
  implementation(libs.appcompat)
  implementation(libs.viewpump)
//    compileOnly 'androidx.appcompat:appcompat:1.0.2'
//    implementation 'io.github.inflationx:viewpump:1.0.0'
//
//    testImplementation 'androidx.annotation:annotation:1.0.0'
//    testImplementation 'androidx.test:runner:1.1.0'
}
