import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask

plugins {
    alias(libs.plugins.versions)
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.ktlint)
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.google.services) apply false
    alias(libs.plugins.firebase.crashlytics.gradle) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.kotlin.android.extensions) apply false
    alias(libs.plugins.navigation.safeargs) apply false
}

subprojects {
    apply(plugin = "org.jlleitschuh.gradle.ktlint")
}

allprojects {
    repositories {
        mavenCentral()
        google()
        maven { url = uri("https://maven.google.com") }
        maven { url = uri("https://jcenter.bintray.com") }
        maven { url = uri("https://jitpack.io") }
    }
}

task<Delete>("clean") {
    delete(rootProject.buildDir)
}

fun isNotStable(version: String): Boolean {
    val versionLower = version.toLowerCase().trim()
    return Regex("-(alpha|rc|beta)[0-9.]*\$").containsMatchIn(versionLower)
}

tasks.withType<DependencyUpdatesTask> {
    rejectVersionIf {
        isNotStable(candidate.version)
    }
}
