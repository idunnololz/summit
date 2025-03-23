include(":app")
include(":thirdPartyModules:calligraphy")
include(":thirdPartyModules:overlappingPane")
include(":thirdPartyModules:imagepicker")
include(":thirdPartyModules:markwon:markwon-core")
include(":thirdPartyModules:markwon:markwon-ext-strikethrough")
include(":thirdPartyModules:markwon:markwon-ext-tables")
include(":thirdPartyModules:markwon:markwon-html")
include(":thirdPartyModules:markwon:markwon-linkify")
include(":thirdPartyModules:markwon:markwon-simple-ext")

rootProject.name = "summit"

pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}
