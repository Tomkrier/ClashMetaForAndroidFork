rootProject.name = "ClashMetaForAndroid"

include(":app")
include(":core")
include(":service")
include(":design")
include(":common")
include(":hideapi")

pluginManagement {
    repositories {
        google()
        mavenLocal()
        mavenCentral()
        gradlePluginPortal()
    }
    plugins {
        id("com.android.legacy-kapt") version "9.4.1"
    }
}