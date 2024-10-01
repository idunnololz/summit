plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
    id("androidx.navigation.safeargs.kotlin")
    id("org.jetbrains.kotlin.plugin.parcelize")
    id("kotlin-kapt")
    id("com.google.devtools.ksp")
    alias(libs.plugins.hilt)
}

android {
    compileSdk = 34
    namespace = "com.idunnololz.summit"

    defaultConfig {
        applicationId = "com.idunnololz.summit"
        minSdk = 21
        targetSdk = 34
        versionCode = 186
        versionName = "1.43.1"

        ksp {
            arg("room.schemaLocation", "$projectDir/schemas")
        }
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
        }
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
    androidResources {
        generateLocaleConfig = true
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

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
    implementation(project(":overlappingPane"))
    implementation(project(":calligraphy"))

    implementation(libs.kotlin.reflect)
    implementation(libs.kotlin.stdlib.jdk7)
    implementation(libs.appcompat)
    implementation(libs.core)
    implementation(libs.activity.ktx)
    implementation(libs.fragment.ktx)
    implementation(libs.material)
    implementation(libs.cardview)
    implementation(libs.recyclerview)
    implementation(libs.preference.ktx)
    implementation(libs.gson)
    implementation(libs.constraintlayout)
    implementation(libs.jsoup)
    implementation(libs.swiperefreshlayout)

    implementation(libs.lifecycle.viewmodel.ktx)
    implementation(libs.lifecycle.extensions)
    implementation(libs.lifecycle.livedata.ktx)

    implementation(libs.media3.core)
    implementation(libs.media3.ui)
    implementation(libs.media3.dash)
    implementation(libs.media3.hls)

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

    implementation(libs.kotlinx.coroutine.android)

    implementation(libs.disklrucache)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.crashlytics)
//    implementation(libs.firebase.analytics)

    implementation(platform(libs.okhttp.bom))
    implementation(libs.okhttp.okhttp)
    implementation(libs.okhttp.logging.intercepter)

    implementation(libs.retrofit2.retrofit)
    implementation(libs.retrofit2.converter.gson)
    implementation(libs.retrofit2.converter.moshi)

    implementation(libs.markwon.core)
    implementation(libs.markwon.ext.tables)
    implementation(libs.markwon.simple.ext)
    implementation(libs.markwon.html)
    implementation(libs.markwon.ext.strikethrough)
    implementation(libs.markwon.linkify)

    implementation(libs.work.runtime.ktx)

    implementation(libs.browser)

    implementation(libs.moshi)
    implementation(libs.moshi.adapter)
    ksp(libs.moshi.kotlin.codegen)

    implementation(libs.flow.layout)

    implementation(libs.arrow.core)

    implementation(libs.hilt.common)
    implementation(libs.hilt.work)
    implementation(libs.core.splashscreen)

    implementation(libs.moshi.sealed.runtime)
    ksp(libs.moshi.sealed.codegen)

    implementation(libs.transformations)
    implementation(libs.imagepicker)
    implementation(libs.java.string.similarity)
    implementation(libs.viewpump)
    implementation(libs.colorpickerview)

    implementation(libs.hilt.android)
    kapt(libs.hilt.android.compiler)
    kapt(libs.hilt.compiler)

    implementation(libs.zoom.layout)
    implementation(libs.process.phoenix)
    implementation(libs.media3.transformer)

    implementation(libs.exifinterface)

    implementation(libs.datastore.preferences)

    coreLibraryDesugaring(libs.desugar.jdk.libs)
}

// Allow references to generated code
kapt {
    correctErrorTypes = true
}
