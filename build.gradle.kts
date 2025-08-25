plugins {
  alias(libs.plugins.android.application) apply false
  alias(libs.plugins.android.library) apply false
  alias(libs.plugins.kotlin.android) apply false
  alias(libs.plugins.ktlint)
  alias(libs.plugins.ksp) apply false
  alias(libs.plugins.hilt) apply false
  alias(libs.plugins.kotlin.android.extensions) apply false
  alias(libs.plugins.navigation.safeargs) apply false
  alias(libs.plugins.jetbrains.kotlin.jvm) apply false
  alias(libs.plugins.kotlin.plugin.serialization) apply false
  alias(libs.plugins.versions)
  alias(libs.plugins.sentry) apply false
}

configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
  version.set("1.7.1")
}

subprojects {
  apply(plugin = "org.jlleitschuh.gradle.ktlint")
}

allprojects {
  repositories {
    maven {
      url = uri("https://sdk.withpersona.com/android/releases")
    }
    mavenCentral()
    google()
    maven { url = uri("https://maven.google.com") }
    maven { url = uri("https://jitpack.io") }
    maven { url = uri("https://s01.oss.sonatype.org/content/repositories/snapshots") }
  }
}

task<Delete>("clean") {
  delete(rootProject.buildDir)
}
