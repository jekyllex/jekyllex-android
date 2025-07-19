import java.io.FileWriter
import java.io.BufferedWriter
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream

import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

import java.security.MessageDigest
import java.security.DigestInputStream

import com.android.build.gradle.internal.api.ApkVariantOutputImpl
import com.android.build.gradle.internal.tasks.factory.dependsOn

plugins {
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsKotlinAndroid)
}

val bootstrapVersion = "v0.1.3"

android {
    namespace = "xyz.jekyllex"
    compileSdk = 34
    ndkVersion = "27.2.12479018"

    defaultConfig {
        applicationId = "xyz.jekyllex"
        minSdk = 24
        targetSdk = 34
        versionCode = 4
        versionName = "v0.2.2"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        buildConfigField("String", "GIT_HASH", getGitHash())
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
            isUniversalApk = true

            reset()
            include("arm64-v8a", "armeabi-v7a", "x86_64", "x86")
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
        allWarningsAsErrors = false
        freeCompilerArgs += listOf("-opt-in=androidx.compose.material3.ExperimentalMaterial3Api")
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

    project.tasks.preBuild.dependsOn("setupBootstraps")

    val abiCodes = mapOf("armeabi-v7a" to 1, "arm64-v8a" to 2, "x86" to 3, "x86_64" to 4)

    applicationVariants.configureEach {
        outputs.configureEach {
            this as ApkVariantOutputImpl
            val abi = filters.find { it.filterType == "ABI" }
            val abiCode = abi?.let { abiCodes[it.identifier] }
            val abiName = abi?.let { "-" + abi.identifier } ?: ""
            val isRelease = buildType.name.lowercase().contains("release")
            versionCodeOverride = abiCode?.let { it * 1000 + versionCode } ?: versionCode
            outputFileName = "${rootProject.name.lowercase()}${
                if (isRelease) "-"
                else "-${buildType.name}-"
            }${defaultConfig.versionName}$abiName.apk"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
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

fun getGitHash(): String {
    val stdout = ByteArrayOutputStream()

    exec {
        commandLine("git", "rev-parse", "--short", "HEAD")
        standardOutput = stdout
    }

    return "\"" + stdout.toString().trim() + "\""
}

fun downloadBootstrap(arch: String, expectedChecksum: String, version: String) {
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

        val checksum = BigInteger(1, digest.digest()).toString(16)
        if (checksum != expectedChecksum) {
            logger.quiet("Deleting old local file with wrong hash: ${zipDownloadFile.absolutePath}")
            zipDownloadFile.delete()
        }
    }

    if (!zipDownloadFile.exists()) {
        val remoteUrl = "https://dl.jekyllex.xyz/ruby/$version/$arch.zip"
        logger.quiet("Downloading $remoteUrl ...")

        zipDownloadFile.parentFile.mkdirs()
        val client = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build()

        val request = HttpRequest.newBuilder()
            .uri(URI.create(remoteUrl))
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

        val checksum = BigInteger(1, digest.digest()).toString(16)
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

                    try {
                        FileOutputStream(targetFile).use {
                            zipInput.copyTo(it)
                            it.close()
                        }
                    } catch (e: Exception ) {
                        println("Error $e")
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

tasks {
    val setupBootstraps by registering {
        if (gradle.startParameter.taskNames.any { it.contains("assembleRelease") }) {
            dependsOn("buildBootstraps")
        } else {
            dependsOn("downloadBootstraps")
        }

        doFirst {
            val map = listOf("aarch64", "i686", "arm", "x86_64")
            map.forEach { arch -> setupBootstrap(arch) }
        }
    }

    val buildBootstraps by register("buildBootstraps", Exec::class) {
        workingDir = file("${project.projectDir}/srcLib")
        standardOutput = System.out
        errorOutput = System.err

        doFirst { delete("srcLib/tmp") }
        commandLine("bash", "build.sh")
    }

    val downloadBootstraps by registering {
        doFirst {
            val map = mapOf(
                "aarch64" to "266b081bb64e33541808e2f627e4667ed8f8ef10a0edbfe736c3338c97930e9b",
                "arm" to "57f7c270d6203323af3d30f626b1c41a1d59d0e6a6cb0b57a5e908c7a6349c35",
                "i686" to "8353c79ca752d754f00da4cd33b6245c253d79a852c8066a1e0809684d178539",
                "x86_64" to "46556fa1b3b690d0c105f7c110f2dd5d57d9a3ab0c29eab2fa3a963d2db41aea"
            )

            map.forEach { (arch, checksum) -> downloadBootstrap(arch, checksum, bootstrapVersion) }
        }
    }
}
