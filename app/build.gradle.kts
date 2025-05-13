import com.android.build.gradle.internal.cxx.configure.gradleLocalProperties
import io.sentry.android.gradle.extensions.InstrumentationFeature
import java.util.EnumSet
import org.jetbrains.kotlin.konan.properties.loadProperties

plugins {
  id("com.android.application")
  id("org.jetbrains.kotlin.android")
  id("androidx.navigation.safeargs.kotlin")
  id("org.jetbrains.kotlin.plugin.parcelize")
  id("com.google.devtools.ksp")
  id("kotlin-kapt")
  id("io.sentry.android.gradle") version "5.3.0"
  alias(libs.plugins.hilt)
  alias(libs.plugins.kotlin.plugin.serialization)
}

android {
  compileSdk = 35
  namespace = "com.idunnololz.summit"

  defaultConfig {
    applicationId = "com.idunnololz.summit"
    minSdk = 21
    targetSdk = 35
    versionCode = 254
    versionName = "1.61.3"

    ksp {
      arg("room.schemaLocation", "$projectDir/schemas")
    }
    buildConfigField(
      "String",
      "SUMMIT_JWT",
      "\"${gradleLocalProperties(rootDir, providers).getProperty("summit.jwt")}\"",
    )
  }
  buildTypes {
    release {
      isMinifyEnabled = true
      proguardFiles(
        getDefaultProguardFile("proguard-android-optimize.txt"),
        "proguard-rules.pro",
      )
    }
    debug {
      versionNameSuffix = "-DEBUG"
      applicationIdSuffix = ".debug"
    }
  }

  buildFeatures {
    viewBinding = true
    buildConfig = true
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17

    isCoreLibraryDesugaringEnabled = true
  }
  kotlin {
    jvmToolchain(17)
  }
}

sentry {
  try {
    val sentryProperties = loadProperties(File(rootDir, "sentry.properties").path)
    if (sentryProperties["auth.token"] == null) {
      autoUploadProguardMapping.set(false)
    }
  } catch (e: Exception) {
    autoUploadProguardMapping.set(false)
  }
  tracingInstrumentation {
    enabled.set(true)
    features.set(
      EnumSet.allOf(InstrumentationFeature::class.java) - InstrumentationFeature.OKHTTP,
    )
  }
}

configurations.configureEach {
  exclude(group = "io.sentry", module = "sentry-android-ndk")
}

dependencies {
  implementation(project(":thirdPartyModules:calligraphy"))
  implementation(project(":thirdPartyModules:imagepicker"))
  implementation(project(":thirdPartyModules:overlappingPane"))
  implementation(project(":thirdPartyModules:markwon:markwon-core"))
  implementation(project(":thirdPartyModules:markwon:markwon-ext-strikethrough"))
  implementation(project(":thirdPartyModules:markwon:markwon-ext-tables"))
  implementation(project(":thirdPartyModules:markwon:markwon-linkify"))
  implementation(project(":thirdPartyModules:markwon:markwon-simple-ext"))
  implementation(project(":thirdPartyModules:zoomLayout"))

  implementation(libs.kotlin.reflect)
  implementation(libs.kotlin.stdlib.jdk7)
  implementation(libs.appcompat)
  implementation(libs.core.ktx)
  implementation(libs.activity.ktx)
  implementation(libs.fragment.ktx)
  implementation(libs.material)
  implementation(libs.cardview)
  implementation(libs.recyclerview)
  implementation(libs.preference.ktx)
  implementation(libs.gson)
  implementation(libs.constraintlayout)
  implementation(libs.ksoup)
  implementation(libs.swiperefreshlayout)
  implementation(libs.transition.ktx)
  implementation(libs.kotlinx.coroutine.android)
  implementation(libs.disklrucache)
  implementation(libs.slidingpanelayout)

  implementation(libs.lifecycle.viewmodel.ktx)
  implementation(libs.lifecycle.extensions)
  implementation(libs.lifecycle.livedata.ktx)

  implementation(libs.media3.core)
  implementation(libs.media3.ui)
  implementation(libs.media3.dash)
  implementation(libs.media3.hls)
  implementation(libs.media3.transformer)

  implementation(libs.navigation.fragment.ktx)
  implementation(libs.navigation.ui.ktx)

  implementation(libs.room.runtime)
  implementation(libs.room.ktx)
  ksp(libs.room.compiler)

  implementation(libs.window)

  implementation(libs.coil)
  implementation(libs.coil.gif)
  implementation(libs.coil.svg)
  implementation(libs.coil.video)
  implementation(libs.coil.okhttp)

  implementation(platform(libs.okhttp.bom))
  implementation(libs.okhttp.okhttp)
  implementation(libs.okhttp.logging.intercepter)

  implementation(libs.retrofit2.retrofit)
  implementation(libs.retrofit2.converter.gson)
  implementation(libs.retrofit2.converter.kotlinx.serialization)

  implementation(libs.work.runtime.ktx)

  implementation(libs.browser)

  implementation(libs.flow.layout)

  implementation(libs.arrow.core)

  implementation(libs.hilt.common)
  implementation(libs.hilt.work)
  implementation(libs.core.splashscreen)

  implementation(libs.transformations)
  implementation(libs.java.string.similarity)
  implementation(libs.viewpump)

  implementation(libs.hilt.android)
  // Tried to switch to KSP + Dagger/Hilt but the build performance was terrible. Specifically
  // for incremental builds. Make sure to test this when switching to KSP again in the future.
  kapt(libs.hilt.android.compiler)

  implementation(libs.process.phoenix)

  implementation(libs.exifinterface)

  implementation(libs.datastore.preferences)

  coreLibraryDesugaring(libs.desugar.jdk.libs)

  implementation(libs.kotlinx.serialization.json)
  implementation(libs.commonmark)
  implementation(libs.asynclayoutinflater)

//    debugImplementation("com.squareup.leakcanary:leakcanary-android:2.14")
}

kapt {
  correctErrorTypes = true
}
