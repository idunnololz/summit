include(":api")
include(":app")
include(":thirdPartyModules:calligraphy")
include(":thirdPartyModules:overlappingPane")
include(":thirdPartyModules:markwon:markwon-core")
include(":thirdPartyModules:markwon:markwon-ext-strikethrough")
include(":thirdPartyModules:markwon:markwon-ext-tables")
include(":thirdPartyModules:markwon:markwon-linkify")
include(":thirdPartyModules:markwon:markwon-simple-ext")
include(":thirdPartyModules:zoomLayout")

rootProject.name = "summit"

pluginManagement {
  repositories {
    gradlePluginPortal()
    google()
    mavenCentral()
  }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
include(":tools")
