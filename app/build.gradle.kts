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
    compileSdk = 33
    namespace = "com.idunnololz.summit"

    defaultConfig {
        applicationId = "com.idunnololz.summit"
        minSdk = 21
        targetSdk = 33
        versionCode = 44
        versionName = "0.1.44"

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
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin {
        jvmToolchain(17)
    }
}

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
    implementation(project(":overlappingPane"))

    implementation(libs.kotlin.stdlib.jdk7)
    implementation(libs.appcompat)
    implementation(libs.core)
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

    implementation(libs.navigation.fragment.ktx)
    implementation(libs.navigation.ui.ktx)
    implementation(libs.navigation.dynamic.features.fragment)

    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    implementation(libs.room.rx)
    ksp(libs.room.compiler)

    implementation(libs.coil)
    implementation(libs.coil.gif)

    implementation(libs.fresco.fresco)
    implementation(libs.fresco.animated.webp)
    implementation(libs.fresco.webp.support)
    implementation(libs.kotlinx.coroutine.android)

    implementation(libs.disklrucache)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.analytics)

    implementation(platform(libs.okhttp.bom))
    implementation(libs.okhttp.okhttp)
    implementation(libs.okhttp.logging.intercepter)

    implementation(libs.retrofit2.retrofit)
    implementation(libs.retrofit2.converter.gson)

    implementation(libs.markwon.core)
    implementation(libs.markwon.ext.tables)
    implementation(libs.markwon.simple.ext)
    implementation(libs.markwon.html)
    implementation(libs.markwon.ext.strikethrough)
    implementation(libs.markwon.linkify)
    implementation(libs.markwon.image.coil)

    implementation(libs.work.runtime.ktx)

    implementation(libs.browser)

    implementation(libs.moshi)
    implementation(libs.moshi.adapter)
    ksp(libs.moshi.kotlin.codegen)

    implementation(libs.flow.layout)

    implementation("io.arrow-kt:arrow-core:1.2.0-RC")

    implementation(libs.hilt.common)
    implementation(libs.hilt.work)
    implementation(libs.core.splashscreen)

    implementation("dev.zacsweers.moshix:moshi-sealed-runtime:0.22.1")
    ksp("dev.zacsweers.moshix:moshi-sealed-codegen:0.22.1")

    implementation("com.github.Commit451.coil-transformations:transformations:2.0.2")
    implementation("com.github.Drjacky:ImagePicker:2.3.22")

    implementation(libs.hilt.android)
    kapt(libs.hilt.android.compiler)

    implementation(libs.threeten.abp)
}

// Allow references to generated code
kapt {
    correctErrorTypes = true
}