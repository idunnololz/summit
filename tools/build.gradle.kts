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
    implementation("com.patreon:patreon:0.4.2")
}

tasks.register<JavaExec>("patreon") {
    workingDir = project.rootProject.projectDir
    mainClass.set("com.example.tools.UpdatePatreonMembers")
    classpath = sourceSets.main.get().runtimeClasspath
}