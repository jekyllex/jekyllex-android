import java.io.FileWriter
import java.io.BufferedWriter
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream

import java.net.URL
import java.net.HttpURLConnection

import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

import java.security.MessageDigest
import java.security.DigestInputStream

import com.android.build.gradle.internal.api.ApkVariantOutputImpl
import com.android.build.gradle.internal.tasks.factory.dependsOn

plugins {
    alias(libs.plugins.googleServices)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.firebaseCrashlytics)
    alias(libs.plugins.jetbrainsKotlinAndroid)
}

val bootstrapVersion = "v0.1.2"

android {
    namespace = "xyz.jekyllex"
    compileSdk = 34
    ndkVersion = "24.0.8215888"

    defaultConfig {
        applicationId = "xyz.jekyllex"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "v0.1.0"

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

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true

            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )

            signingConfig = signingConfigs.getByName("release")
        }

        create("staging") {
            initWith(getByName("debug"))

            ndk {
                abiFilters.add("arm64-v8a")
            }
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

    applicationVariants.configureEach {
        // rename the output APK file
        outputs.configureEach {
            (this as ApkVariantOutputImpl).outputFileName =
                "${rootProject.name.lowercase()}${
                    if (buildType.name == "release") "-"
                    else "-${buildType.name}-"
                }${defaultConfig.versionName}.apk"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.compose.preference)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.messaging)
    implementation(libs.firebase.crashlytics)
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

// Adapted from https://github.com/termux/termux-app/blob/android-10/app/build.gradle#L84
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
            val map = mapOf(
                "aarch64" to "2a93c51def0ad9dbf1ba4aa43a6d6f6bc8bfcc453744a2b02ea1fe4257beeb6c",
                "arm" to "e71ffdd400147e228c22449d93e642736a7b2f989767fc8fe78311dfdf3f04b6",
                "i686" to "e1b9410140c455a2309ef727da10c9337fb846a837c7165796268ff311539a57",
                "x86_64" to "966a0ac9bcb166de536ad26b84005efffff5e0b70c23831c8535238947ed214f"
            )

            map.forEach { (arch, checksum) -> setupBootstrap(arch, checksum, bootstrapVersion) }
        }
    }
}
