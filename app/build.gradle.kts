plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
    id("androidx.navigation.safeargs.kotlin")
    id("org.jetbrains.kotlin.plugin.parcelize")
    id("kotlin-kapt")
    id("com.google.devtools.ksp")
}

android {
    compileSdk = 33
    namespace = "com.idunnololz.summit"

    defaultConfig {
        applicationId = "com.idunnololz.summit"
        minSdk = 21
        targetSdk = 33
        versionCode = 1
        versionName = "1.0.0"
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
    implementation(libs.kotlin.stdlib.jdk7)
    implementation(libs.appcompat)
    implementation(libs.core)
    implementation(libs.fragment.ktx)
    implementation(libs.okhttp)
    implementation(libs.material)
    implementation(libs.cardview)
    implementation(libs.recyclerview)
    implementation(libs.preference.ktx)
    implementation(libs.gson)
    implementation(libs.constraintlayout)
    implementation(libs.lifecycle.extensions)
    implementation(libs.jsoup)
    implementation(libs.swiperefreshlayout)

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

    implementation(libs.fresco.fresco)
    implementation(libs.fresco.animated.webp)
    implementation(libs.fresco.webp.support)
    implementation(libs.kotlinx.coroutine.android)

    implementation(libs.disklrucache)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.analytics)

    implementation(libs.markwon.core)
    implementation(libs.markwon.ext.tables)
    implementation(libs.markwon.simple.ext)
    implementation(libs.markwon.html)
    implementation(libs.markwon.ext.strikethrough)
    implementation(libs.markwon.linkify)

    implementation(libs.work.runtime.ktx)

    implementation(libs.browser)

    // TODO: Remove the following deps
    implementation("org.apmem.tools:layouts:1.10@aar")
    implementation("commons-io:commons-io:2.6")
    implementation("de.hdodenhof:circleimageview:3.1.0")
    implementation("org.apache.commons:commons-lang3:3.11")
    implementation(libs.glide)

    implementation(libs.rx.rxjava)
    implementation(libs.rx.rxandroid)
    implementation(libs.work.rxjava2)
}
