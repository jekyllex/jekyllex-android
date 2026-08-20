import java.io.FileWriter
import java.io.BufferedWriter
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.BufferedOutputStream
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

import java.security.MessageDigest
import java.security.DigestInputStream
import java.time.Duration

import com.android.build.gradle.internal.api.ApkVariantOutputImpl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsKotlinAndroid)
}

val bootstrapVersion = "v0.1.5"
val targetABI = findProperty("targetABI") as? String
val abiCodes = mapOf("armeabi-v7a" to 1, "arm64-v8a" to 2, "x86" to 3, "x86_64" to 4)

android {
    namespace = "xyz.jekyllex"
    compileSdk {
        version = release(37) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "xyz.jekyllex"
        minSdk = 24
        targetSdk = 36
        versionCode = 6
        versionName = "v0.2.4"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        buildConfigField("String", "GIT_HASH", project.gitHash())
        buildConfigField("String", "BOOTSTRAP", "\"$bootstrapVersion\"")
    }

    signingConfigs {
        create("release") {
            storeFile = file("keystore.jks")
            keyAlias = System.getenv("KEYSTORE_ALIAS")
            keyPassword = System.getenv("KEYSTORE_PASSWORD")
            storePassword = System.getenv("KEYSTORE_PASSWORD")
        }
    }

    splits {
        abi {
            isEnable = true
            reset()
            if (!targetABI.isNullOrEmpty()) {
                include(targetABI)
            } else {
                isUniversalApk = true
                include("arm64-v8a", "armeabi-v7a", "x86_64", "x86")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true

            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }

        create("githubRelease") {
            initWith(getByName("release"))
            signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }

        jniLibs {
            useLegacyPackaging = true
            keepDebugSymbols += "*/**/*.so"
        }
    }

    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }

    applicationVariants.configureEach {
        outputs.configureEach {
            this as ApkVariantOutputImpl
            val abi = filters.find { it.filterType == "ABI" }
            val abiCode = abi?.let { abiCodes[it.identifier] }
            val abiName =  "-" + (abi?.let { abi.identifier } ?: "universal")
            val isRelease = buildType.name.lowercase().contains("release")
            versionCodeOverride = versionCode * 10 + (abiCode ?: 0)
            outputFileName = "${rootProject.name.lowercase()}${
                if (isRelease) "-" else "-${buildType.name}-"
            }${defaultConfig.versionName}$abiName.apk"
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
        allWarningsAsErrors.set(false)
        freeCompilerArgs.add("-opt-in=androidx.compose.material3.ExperimentalMaterial3Api")
    }
}

val skipBootstrap = gradle.startParameter.taskNames.isNotEmpty() &&
    gradle.startParameter.taskNames.all { name ->
        val task = name.substringAfterLast(":")
        task.startsWith("test") && !task.contains("AndroidTest", ignoreCase = true)
    }

if (!skipBootstrap) {
    tasks.named("preBuild").configure {
        dependsOn("setupBootstraps")
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.documentfile)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.compose.preference)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.leakcanary)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}

fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }

fun Project.gitHash(): String {
    val hash = providers.exec {
        commandLine("git", "rev-parse", "--short", "HEAD")
    }.standardOutput.asText.get().trim()
    return "\"$hash\""
}

fun downloadBootstrap(arch: String, expectedChecksum: String) {
    val buffer = ByteArray(8192)
    val digest = MessageDigest.getInstance("SHA-256")
    val zipDownloadFile = File(project.rootDir, "bootstraps/ruby-${arch}.zip")

    if (zipDownloadFile.exists()) {
        FileInputStream(zipDownloadFile).use { input ->
            var bytesRead: Int
            while (input.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
        }

        val checksum = digest.digest().toHex()
        if (checksum != expectedChecksum) {
            logger.quiet("Deleting old local file with wrong hash: ${zipDownloadFile.absolutePath}")
            File("${zipDownloadFile.absolutePath}.done").delete()
            zipDownloadFile.delete()
        }
    }

    if (!zipDownloadFile.exists()) {
        val remoteUrl = "https://dl.jekyllex.xyz/ruby/$bootstrapVersion/$arch.zip"
        logger.quiet("Downloading $remoteUrl ...")

        zipDownloadFile.parentFile.mkdirs()
        val client = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(30))
            .build()

        val request = HttpRequest.newBuilder()
            .uri(URI.create(remoteUrl))
            .timeout(Duration.ofMinutes(5))
            .GET()
            .build()

        val response = client.send(request, HttpResponse.BodyHandlers.ofInputStream())
        if (response.statusCode() != 200) {
            throw GradleException("Failed to download $remoteUrl: HTTP ${response.statusCode()}")
        }

        DigestInputStream(response.body(), digest).use { input ->
            BufferedOutputStream(FileOutputStream(zipDownloadFile)).use { out ->
                var bytesRead: Int
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    out.write(buffer, 0, bytesRead)
                }
            }
        }

        val checksum = digest.digest().toHex()
        if (checksum != expectedChecksum) {
            zipDownloadFile.delete()
            throw GradleException("Wrong checksum for $remoteUrl: expected: $expectedChecksum, actual: $checksum")
        }
    }
}

// Adapted from https://github.com/termux/termux-app/blob/android-10/app/build.gradle#L84
fun setupBootstrap(arch: String) {
    val zipDownloadFile = File(project.rootDir, "bootstraps/ruby-${arch}.zip")

    val doneMarkerFile = File("${zipDownloadFile.absolutePath}.done")
    if (doneMarkerFile.exists()) return

    val archMap = mapOf(
        "i686" to "x86",
        "x86_64" to "x86_64",
        "arm" to "armeabi-v7a",
        "aarch64" to "arm64-v8a"
    )

    val archDirName = archMap[arch]

    val outputPath = "${project.rootDir.absolutePath}/app/src/main/jniLibs/$archDirName"
    val outputDir = File(outputPath).absoluteFile
    if (!outputDir.exists()) outputDir.mkdirs()

    println("Setting up bootstrap for $arch at $outputPath")

    val symlinksFile = File(outputDir, "libsymlinks.so").absoluteFile
    if (symlinksFile.exists()) symlinksFile.delete()

    val mappingsFile = File(outputDir, "libfiles.so").absoluteFile
    if (mappingsFile.exists()) mappingsFile.delete()

    var counter = 100
    mappingsFile.createNewFile()
    val mappingsFileWriter = BufferedWriter(FileWriter(mappingsFile))

    FileInputStream(zipDownloadFile).use { fileInput ->
        ZipInputStream(fileInput).use { zipInput ->
            var zipEntry: ZipEntry? = zipInput.nextEntry

            while (zipEntry != null) {
                if (zipEntry.getName() == "SYMLINKS.txt") {
                    FileOutputStream(symlinksFile).use {
                        zipInput.copyTo(it)
                        it.close()
                    }
                } else if (!zipEntry.isDirectory) {
                    val soName = "lib$counter.so"
                    val targetFile = File(outputDir, soName).absoluteFile

                    FileOutputStream(targetFile).use {
                        zipInput.copyTo(it)
                    }

                    mappingsFileWriter.write("$soName←${zipEntry.name}\n")
                    counter++
                }

                zipEntry = zipInput.nextEntry
            }

        }
    }

    mappingsFileWriter.close()
    doneMarkerFile.createNewFile()
}

val archMap = mapOf(
    "x86" to "i686",
    "x86_64" to "x86_64",
    "armeabi-v7a" to "arm",
    "arm64-v8a" to "aarch64"
)

tasks.register("setupBootstraps") {
    if (gradle.startParameter.taskNames.any { it.contains("assembleRelease") }) {
        dependsOn("buildBootstraps")
    } else {
        dependsOn("downloadBootstraps")
    }

    doFirst {
        if (targetABI.isNullOrEmpty()) {
            archMap.values.forEach { arch -> setupBootstrap(arch) }
        } else {
            setupBootstrap(archMap.getValue(targetABI))
        }
    }
}

tasks.register<Exec>("buildBootstraps") {
    workingDir = file("${project.projectDir}/srcLib")
    standardOutput = System.out
    errorOutput = System.err

    doFirst { delete("srcLib/tmp") }
    if (targetABI.isNullOrEmpty()) commandLine("bash", "build.sh")
    else commandLine("bash", "build.sh", "-a", archMap.getValue(targetABI))
}

tasks.register("downloadBootstraps") {
    doFirst {
        val map = mapOf(
            "aarch64" to "81a8beb55a352eb7cb726e4e71bf6be40fe7e0aeed7819233a6c8ce70bdd40d4",
            "arm" to "6152aedad758906e0a2ab32d4679b709ac8297bf3f028e0cd7e7825ae5a15df9",
            "i686" to "1e23f67dc1efa66af6384537f914b81ce21c33ace605806fa821f42d34d4eaa4",
            "x86_64" to "8f073ca660ddd84a8568cdb05b5b6eafa2295d3f8fe5dd255fdda4247ee8c782"
        )

        if (targetABI.isNullOrEmpty()) {
            map.forEach { (arch, checksum) -> downloadBootstrap(arch, checksum) }
        } else {
            val arch = archMap.getValue(targetABI)
            downloadBootstrap(arch, map.getValue(arch))
        }
    }
}
