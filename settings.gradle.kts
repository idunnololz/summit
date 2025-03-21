include(":app")
include(":calligraphy")
include(":overlappingPane")
include(":imagepicker")
include(":markwon:markwon-core")
include(":markwon:markwon-ext-strikethrough")
include(":markwon:markwon-ext-tables")
include(":markwon:markwon-html")
include(":markwon:markwon-linkify")
include(":markwon:markwon-simple-ext")

rootProject.name = "summit"

pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}
