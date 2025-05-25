plugins {
  id("java-library")
  alias(libs.plugins.jetbrains.kotlin.jvm)
}
java {
  sourceCompatibility = JavaVersion.VERSION_17
  targetCompatibility = JavaVersion.VERSION_17
}
kotlin {
  compilerOptions {
    jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17
  }
}

dependencies {
  implementation(libs.tools.patreon)
}

tasks.register<JavaExec>("patreon") {
  workingDir = project.rootProject.projectDir
  mainClass.set("com.idunnololz.tools.UpdatePatreonMembers")
  classpath = sourceSets.main.get().runtimeClasspath
}
