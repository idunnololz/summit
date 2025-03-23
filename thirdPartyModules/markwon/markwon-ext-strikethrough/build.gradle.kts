plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "io.noties.markwon.ext.strikethrough"
    compileSdk = 35

    defaultConfig {
        minSdk = 21
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
    implementation(project(":thirdPartyModules:markwon:markwon-core"))
    implementation(libs.annotation)
    implementation(libs.commonmark)
    implementation(libs.commonmark.strikethrough)
    implementation(libs.core)
}
