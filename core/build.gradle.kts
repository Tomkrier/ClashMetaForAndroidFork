import com.github.kr328.golang.GolangBuildTask
import java.io.File
import org.gradle.api.file.Directory

plugins {
    id("com.android.library")
    id("kotlinx-serialization")
}

val golangSource = file("src/main/golang/native")

android {
    productFlavors {
        all {
            externalNativeBuild {
                cmake {
                    arguments("-DGO_SOURCE:STRING=${golangSource}")
                    arguments("-DGO_OUTPUT:STRING=${project.layout.buildDirectory.get().asFile}/outputs/golang")
                    arguments("-DFLAVOR_NAME:STRING=$name")
                }
            }
        }
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
        }
    }
}

dependencies {
    implementation(project(":common"))

    implementation(libs.androidx.core)
    implementation(libs.kotlin.coroutine)
    implementation(libs.kotlin.serialization.json)
}

afterEvaluate {
    tasks.withType(GolangBuildTask::class.java).configureEach {
        inputs.dir(golangSource)
    }
}

val abis = listOf("arm64-v8a" to "Arm64V8a", "armeabi-v7a" to "ArmeabiV7a", "x86" to "X86", "x86_64" to "X8664")

androidComponents.onVariants { variant ->
    val cmakeName = if (variant.buildType == "debug") "Debug" else "RelWithDebInfo"
    val variantCap = variant.name.replaceFirstChar { it.uppercase() }
    val isDebug = variant.buildType == "debug"

    abis.forEach { (abi, goAbi) ->
        val taskName = "externalGolangBuild$variantCap$goAbi"
        val outputDir = file("${project.layout.buildDirectory.get().asFile}/outputs/golang/${variant.name}")
        val sourceDir = file("src/foss/golang")
        val tags = mutableListOf("foss", "with_gvisor", "cmfa")
        if (isDebug) tags.add("debug")

        val (targetTriple, goArch, goArm) = when (abi) {
            "arm64-v8a" -> Triple("aarch64-linux-android", "arm64", "")
            "armeabi-v7a" -> Triple("armv7a-linux-androideabi", "arm", "7")
            "x86" -> Triple("i686-linux-android", "386", "")
            "x86_64" -> Triple("x86_64-linux-android", "amd64", "")
            else -> Triple("aarch64-linux-android", "arm64", "")
        }

        val osName = System.getProperty("os.name").lowercase()
        val hostTag = when {
            osName.contains("win") -> "windows-x86_64"
            osName.contains("mac") -> "darwin-x86_64"
            else -> "linux-x86_64"
        }

        val ndkDirProvider = androidComponents.sdkComponents.ndkDirectory

        tasks.register(taskName, GolangBuildTask::class.java) {
            this.golangSource.set(sourceDir)
            this.golangOutput.set(outputDir)
            this.tags.set(tags)

            workingDir(sourceDir)

            val ndkDir = ndkDirProvider.get().asFile
            val ccExecutable = File(ndkDir, "toolchains/llvm/prebuilt/$hostTag/bin/${targetTriple}23-clang")

            environment(
                mapOf(
                    "CC" to ccExecutable.absolutePath,
                    "GOOS" to "android",
                    "GOARCH" to goArch,
                    "GOARM" to goArm,
                    "CGO_ENABLED" to "1",
                    "CFLAGS" to "-O3 -Werror"
                )
            )

            val outputFile = File(outputDir, "libclash.so")
            val cmd = mutableListOf("go", "build", "-buildmode", "c-shared", "-trimpath", "-o", outputFile.absolutePath)
            if (tags.isNotEmpty()) {
                cmd.add("-tags")
                cmd.add(tags.joinToString(","))
            }
            if (!isDebug) {
                cmd.add("-ldflags")
                cmd.add("-s -w")
            }
            cmd.add("cfa/native")
            commandLine(cmd)
        }

        tasks.configureEach {
            if (name.startsWith("buildCMake$cmakeName[$abi]")) {
                dependsOn(taskName)
                println("Set up dependency: $name -> $taskName")
            }
        }
    }
}