plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.parcelize")
}

android {
    namespace = "com.github.drjacky.imagepicker"
    compileSdk = 35

    defaultConfig {
        minSdk = 21
    }
    buildFeatures {
        viewBinding = true
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
    implementation(libs.kotlin.stdlib.jdk7)
    implementation(libs.core)
    implementation(libs.appcompat)
    implementation(libs.exifinterface)
    implementation(libs.documentfile)
    implementation(libs.lifecycle.viewmodel.ktx)
    implementation(libs.navigation.fragment.ktx)

    implementation(libs.ucropnedit)
}