plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    compileSdk = 34
    namespace = "io.github.inflationx.calligraphy3"

    defaultConfig {
        minSdk = 21

        consumerProguardFiles("consumer-proguard-rules.txt")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android.txt"),
                "consumer-proguard-rules.txt",
            )
        }
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
