import java.io.FileWriter
import java.io.BufferedWriter
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.BufferedOutputStream

import java.net.URL
import java.net.HttpURLConnection

import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

import java.security.MessageDigest
import java.security.DigestInputStream

import com.android.build.gradle.internal.api.ApkVariantOutputImpl
import com.android.build.gradle.internal.tasks.factory.dependsOn

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsKotlinAndroid)
}

android {
    namespace = "xyz.jekyllex"
    compileSdk = 34
    ndkVersion = "24.0.8215888"

    defaultConfig {
        applicationId = "xyz.jekyllex"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        externalNativeBuild {
            ndkBuild {
                cFlags += listOf(
                    "-std=c11",
                    "-Wall",
                    "-Wextra",
                    "-Werror",
                    "-Os",
                    "-fno-stack-protector",
                    "-Wl,--gc-sections"
                )
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }

        jniLibs {
            useLegacyPackaging = true
        }
    }

    project.tasks.preBuild.dependsOn("setupBootstraps")

    splits {
        abi {
            isEnable = true
            reset()
            include("x86", "x86_64", "armeabi-v7a", "arm64-v8a")
        }
    }

    applicationVariants.configureEach {
        val archMap = mapOf(
            "x86" to "i686",
            "x86_64" to "x86_64",
            "armeabi-v7a" to "arm",
            "arm64-v8a" to "aarch64"
        )

        // rename the output APK file
        outputs.configureEach {
            (this as? ApkVariantOutputImpl)?.outputFileName =
                "${rootProject.name.lowercase()}-${
                    archMap[filters.find { it.filterType == "ABI" }?.identifier] ?: "universal"
                }${if (buildType.name == "debug") "-debug" else ""}.apk"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}

fun setupBootstrap(arch: String, expectedChecksum: String, version: String) {
    val digest = MessageDigest.getInstance("SHA-256")
    val zipDownloadFile = File(project.rootDir, "bootstraps/ruby-${arch}.zip")

    if (zipDownloadFile.exists()) {
        val buffer = ByteArray(8192)
        val input = FileInputStream(zipDownloadFile)

        do {
            val bytesRead = input.read(buffer)
            if (bytesRead > 0) digest.update(buffer, 0, bytesRead)
        } while (bytesRead > 0)

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
        val out = BufferedOutputStream(FileOutputStream(zipDownloadFile))

        val connection = (URL(remoteUrl).openConnection() as HttpURLConnection).apply {
            instanceFollowRedirects = true
        }

        val digestStream = DigestInputStream(connection.inputStream, digest)
        val buffer = ByteArray(8192)

        do {
            val bytesRead = digestStream.read(buffer)
            if (bytesRead > 0) out.write(buffer, 0, bytesRead)
        } while (bytesRead > 0)

        out.close()

        val checksum = BigInteger(1, digest.digest()).toString(16)

        if (checksum != expectedChecksum) {
            zipDownloadFile.delete()
            throw GradleException("Wrong checksum for $remoteUrl: expected: $expectedChecksum, actual: $checksum")
        }
    }

    val doneMarkerFile = File("${zipDownloadFile.absolutePath}.$expectedChecksum.done")
    if (doneMarkerFile.exists()) return

    val archMap = mapOf(
        "i686" to "x86",
        "x86_64" to "x86_64",
        "arm" to "armeabi-v7a",
        "aarch64" to "arm64-v8a"
    )

    val archDirName = archMap[arch]

    val outputPath = "${project.rootDir.absolutePath}/app/src/main/jniLibs/$archDirName/"
    val outputDir = File(outputPath).absoluteFile
    if (!outputDir.exists()) outputDir.mkdirs()

    val symlinksFile = File(outputDir, "libsymlinks.so").absoluteFile
    if (symlinksFile.exists()) symlinksFile.delete()

    val mappingsFile = File(outputDir, "libfiles.so").absoluteFile
    if (mappingsFile.exists()) mappingsFile.delete()
    mappingsFile.createNewFile()
    val mappingsFileWriter = BufferedWriter(FileWriter(mappingsFile))

    var counter = 100
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

                    println("target file path is $targetFile")

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
        doFirst {
            setupBootstrap(
                "aarch64",
                "6862d6b545de050791321e13c157a2e1a50eb29ab7698345626d55f3dc92ee42",
                "v0.1.0"
            )
            setupBootstrap(
                "arm",
                "1e509958eea7dde58892db8264375238c06a4c57d4f7b69cab3292d5a77042b4",
                "v0.1.0"
            )
            setupBootstrap(
                "i686",
                "dc61c525a52f3ed466d946fc4df42e43aac35aab32629e7fba5e8602d1f9b4f2",
                "v0.1.0"
            )
            setupBootstrap(
                "x86_64",
                "5a0f49f6ac3242724821801d8849d113ecc0e6ba12df4aa1a3b9773ebe219cf8",
                "v0.1.0"
            )
        }
    }
}
