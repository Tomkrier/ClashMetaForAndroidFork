@file:Suppress("UNUSED_VARIABLE")

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.LibraryExtension
import com.android.build.gradle.BaseExtension
import java.net.URL
import java.util.*

buildscript {
    repositories {
        mavenCentral()
        google()
        maven("https://raw.githubusercontent.com/MetaCubeX/maven-backup/main/releases")
    }
    dependencies {
        classpath(libs.build.android)
        classpath(libs.build.kotlin.common)
        classpath(libs.build.kotlin.serialization)
        classpath(libs.build.ksp)
        classpath(libs.build.golang)
    }
}

subprojects {
    repositories {
        mavenCentral()
        google()
        maven("https://raw.githubusercontent.com/MetaCubeX/maven-backup/main/releases")
    }

    val isApp = name == "app"

    apply(plugin = if (isApp) "com.android.application" else "com.android.library")

    fun queryConfigProperty(key: String): Any? {
        val localProperties = Properties()
        val localPropertiesFile = rootProject.file("local.properties")
        if (localPropertiesFile.exists()) {
            localProperties.load(localPropertiesFile.inputStream())
        } else {
            return null
        }
        return localProperties.getProperty(key)
    }

    if (isApp) {
        extensions.configure<ApplicationExtension> {
            namespace = "com.github.kr328.clash"
            compileSdk = 37
            buildToolsVersion = "37.0.0"
            ndkVersion = "30.0.16248370"

            buildFeatures {
                buildConfig = true
                dataBinding = true
                resValues = true
            }

            defaultConfig {
                val customApplicationId = queryConfigProperty("custom.application.id") as? String?
                applicationId = customApplicationId.takeIf { it?.isNotBlank() == true } ?: "com.github.metacubex.clash"

                minSdk = 23
                targetSdk = 37
                versionName = "2.11.35"
                versionCode = 211035

                resValue("string", "release_name", "v$versionName")
                resValue("integer", "release_code", "$versionCode")

                ndk {
                    abiFilters.add("arm64-v8a")
                }

                externalNativeBuild {
                    cmake {
                        abiFilters("arm64-v8a")
                    }
                }

                project.extensions.getByType<BasePluginExtension>().archivesName.set("cmfa-$versionName")
            }

            packaging {
                resources {
                    excludes.add("DebugProbesKt.bin")
                }
            }

            flavorDimensions += "feature"

            productFlavors {
                val removeSuffix = (queryConfigProperty("remove.suffix") as? String)?.toBoolean() == true

                create("alpha") {
                    dimension = "feature"
                    if (!removeSuffix) {
                        versionNameSuffix = ".Alpha"
                        applicationIdSuffix = ".alpha"
                    }

                    buildConfigField("boolean", "PREMIUM", "false")

                    resValue("string", "launch_name", "@string/launch_name_alpha")
                    resValue("string", "application_name", "@string/application_name_alpha")
                }

                create("meta") {
                    dimension = "feature"
                    if (!removeSuffix) {
                        versionNameSuffix = ".Meta"
                        applicationIdSuffix = ".meta"
                    }

                    buildConfigField("boolean", "PREMIUM", "false")

                    resValue("string", "launch_name", "@string/launch_name_meta")
                    resValue("string", "application_name", "@string/application_name_meta")
                }
            }

            sourceSets {
                getByName("meta") {
                    java.srcDirs("src/foss/java")
                }
                getByName("alpha") {
                    java.srcDirs("src/foss/java")
                }
            }

            signingConfigs {
                val keystore = rootProject.file("signing.properties")
                if (keystore.exists()) {
                    create("release") {
                        val prop = Properties().apply {
                            keystore.inputStream().use(this::load)
                        }

                        storeFile = rootProject.file("release.keystore")
                        storePassword = prop.getProperty("keystore.password")!!
                        keyAlias = prop.getProperty("key.alias")!!
                        keyPassword = prop.getProperty("key.password")!!
                    }
                }
            }

            buildTypes {
                named("release") {
                    isMinifyEnabled = true
                    isShrinkResources = true
                    signingConfig = signingConfigs.findByName("release") ?: signingConfigs["debug"]
                    proguardFiles(
                        getDefaultProguardFile("proguard-android-optimize.txt"),
                        "proguard-rules.pro"
                    )
                }
                named("debug") {
                    versionNameSuffix = ".debug"
                }
            }

            splits {
                abi {
                    isEnable = true
                    isUniversalApk = true
                    reset()
                    include("arm64-v8a")
                }
            }

            compileOptions {
                sourceCompatibility = JavaVersion.VERSION_25
                targetCompatibility = JavaVersion.VERSION_25
            }
        }
    } else {
        extensions.configure<LibraryExtension> {
            namespace = "com.github.kr328.clash.$name"
            compileSdk = 37
            buildToolsVersion = "37.0.0"
            ndkVersion = "30.0.16248370"

            buildFeatures {
                buildConfig = true
                dataBinding = name != "hideapi"
                resValues = true
            }

            defaultConfig {
                minSdk = 23

                resValue("string", "release_name", "v2.11.35")
                resValue("integer", "release_code", "211035")

                ndk {
                    abiFilters.add("arm64-v8a")
                }

                externalNativeBuild {
                    cmake {
                        abiFilters("arm64-v8a")
                    }
                }

                consumerProguardFiles("consumer-rules.pro")
            }

            flavorDimensions += "feature"

            productFlavors {
                create("alpha") {
                    dimension = "feature"

                    buildConfigField("boolean", "PREMIUM", "false")

                    resValue("string", "launch_name", "@string/launch_name_alpha")
                    resValue("string", "application_name", "@string/application_name_alpha")
                }

                create("meta") {
                    dimension = "feature"

                    buildConfigField("boolean", "PREMIUM", "false")

                    resValue("string", "launch_name", "@string/launch_name_meta")
                    resValue("string", "application_name", "@string/application_name_meta")
                }
            }

            sourceSets {
                getByName("meta") {
                    java.srcDirs("src/foss/java")
                }
                getByName("alpha") {
                    java.srcDirs("src/foss/java")
                }
            }

            signingConfigs {
                val keystore = rootProject.file("signing.properties")
                if (keystore.exists()) {
                    create("release") {
                        val prop = Properties().apply {
                            keystore.inputStream().use(this::load)
                        }

                        storeFile = rootProject.file("release.keystore")
                        storePassword = prop.getProperty("keystore.password")!!
                        keyAlias = prop.getProperty("key.alias")!!
                        keyPassword = prop.getProperty("key.password")!!
                    }
                }
            }

            buildTypes {
                named("release") {
                    isMinifyEnabled = false
                    signingConfig = signingConfigs.findByName("release") ?: signingConfigs["debug"]
                    proguardFiles(
                        getDefaultProguardFile("proguard-android-optimize.txt"),
                        "proguard-rules.pro"
                    )
                }
                named("debug") {
                }
            }

            compileOptions {
                sourceCompatibility = JavaVersion.VERSION_25
                targetCompatibility = JavaVersion.VERSION_25
            }
        }
    }
}

task("clean", type = Delete::class) {
    delete(rootProject.buildDir)
}

tasks.wrapper {
    distributionType = Wrapper.DistributionType.ALL

    doLast {
        val sha256 = URL("$distributionUrl.sha256").openStream()
            .use { it.reader().readText().trim() }

        file("gradle/wrapper/gradle-wrapper.properties")
            .appendText("distributionSha256Sum=$sha256")
    }
}