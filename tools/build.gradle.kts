plugins {
  id("java-library")
  alias(libs.plugins.jetbrains.kotlin.jvm)
  alias(libs.plugins.kotlin.plugin.serialization)
}
java {
  sourceCompatibility = JavaVersion.VERSION_17
  targetCompatibility = JavaVersion.VERSION_17
}
kotlin {
  compilerOptions {
    jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17
    freeCompilerArgs.add("-Xannotation-default-target=param-property")
  }
}

dependencies {
  implementation(libs.tools.patreon)
  implementation(libs.retrofit2.retrofit)
  implementation(libs.retrofit2.converter.kotlinx.serialization)
  implementation(libs.kotlinx.serialization.json)
}

tasks.register<JavaExec>("patreon") {
  workingDir = project.rootProject.projectDir
  mainClass.set("com.idunnololz.tools.UpdatePatreonMembers")
  classpath = sourceSets.main.get().runtimeClasspath
}

tasks.register<JavaExec>("instances") {
  workingDir = project.rootProject.projectDir
  mainClass.set("com.idunnololz.tools.UpdateInstanceData")
  classpath = sourceSets.main.get().runtimeClasspath
}
