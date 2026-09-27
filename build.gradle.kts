import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFramework
import java.security.MessageDigest
import java.io.FileInputStream
import java.util.zip.ZipOutputStream
import java.util.zip.ZipEntry
import kotlinx.kover.gradle.plugin.dsl.CoverageUnit

val reproducibleZipEntryTime = 0L

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.detekt)
    alias(libs.plugins.kover)
    // Публикации раскладывают собранную библиотеку в хранилище Maven: из него
    // выпуск упаковывает zip для Gradle, в Maven Central кодек не выгружается.
    `maven-publish`
}

group = "io.github.texport"
// Линия версии; номер выпуска (1.3.0, 1.3.1, …) назначает выпуск по меткам git
// и передаёт свойством -PreleaseVersion. Без него — <линия>.0-SNAPSHOT.
val versionLine = "1.3"
version = providers.gradleProperty("releaseVersion").orNull ?: "$versionLine.0-SNAPSHOT"

repositories {
    mavenLocal()
    google()
    mavenCentral()
}

dependencies {
    detektPlugins(libs.detekt.formatting)
}

detekt {
    config.setFrom(files("$rootDir/config/detekt/detekt.yml"))
    buildUponDefaultConfig = true
    allRules = true
    // Автоправка переписывает исходники на месте; на CI её правки пропадают
    // вместе с раннером, а исправленное нарушение проверку не роняет.
    autoCorrect = System.getenv("CI") == null
    source.setFrom(files("src/commonMain/kotlin", "src/jvmMain/kotlin", "src/androidMain/kotlin", "src/iosMain/kotlin"))
}

kotlin {
    jvm()
    android {
        namespace = "kz.mybrain.ofdcodec"
        compileSdk = libs.versions.androidCompileSdk.get().toInt()
        minSdk = libs.versions.androidMinSdk.get().toInt()

        withHostTest {}
    }
    
    val xcf = XCFramework("OfdProtoCodec")
    listOf(iosArm64(), iosX64(), iosSimulatorArm64()).forEach { target ->
        target.binaries.framework {
            baseName = "OfdProtoCodec"
            xcf.add(this)
        }
    }

    jvmToolchain(libs.versions.javaTargetCore.get().toInt())

    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.ofd.kt.proto)
                implementation(libs.ofd.kt.proto.v204)
                implementation(libs.wire.runtime)
            }
        }
        commonTest {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.ofd.network.client)
            }
        }
    }

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
    }

    tasks.withType<Javadoc>().configureEach {
        options {
            encoding = "UTF-8"
            if (this is StandardJavadocDocletOptions) {
                addStringOption("Xdoclint:none", "-quiet")
            }
        }
    }
}

kover {
    reports {
        verify {
            rule {
                bound {
                    coverageUnits = CoverageUnit.INSTRUCTION
                    minValue = 90
                }
                bound {
                    coverageUnits = CoverageUnit.BRANCH
                    minValue = 94
                }
                bound {
                    coverageUnits = CoverageUnit.LINE
                    minValue = 99
                }
            }
        }
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask<*>>().configureEach {
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }
}

tasks.named("check") {
    dependsOn("koverVerify")
}

tasks.register("generateSpmManifest") {
    group = "publishing"
    description = "Zips OfdProtoCodec XCFramework, calculates SHA-256 and writes Package.swift"
    dependsOn("assembleOfdProtoCodecReleaseXCFramework")

    val versionStr = version.toString()
    val packageSwiftFile = rootProject.file("Package.swift")
    val outputDirectory = layout.buildDirectory.dir("XCFrameworks/release")

    doLast {
        val repoUrl = "https://github.com/texport/ofd-proto-codec"
        val zipName = "OfdProtoCodec.xcframework.zip"
        val outputDir = outputDirectory.get().asFile
        val xcframeworkDir = File(outputDir, "OfdProtoCodec.xcframework")
        val zipFile = File(outputDir, zipName)

        if (!xcframeworkDir.exists()) {
            throw GradleException("XCFramework not found at ${xcframeworkDir.absolutePath}")
        }

        // 1. Zipping XCFramework
        println("Zipping XCFramework to ${zipFile.absolutePath}...")
        zipFile.delete()
        ZipOutputStream(zipFile.outputStream().buffered()).use { zos ->
            xcframeworkDir.walkTopDown()
                .filter { it.isFile }
                .sortedBy { it.relativeTo(xcframeworkDir.parentFile).path }
                .forEach { file ->
                    val relativePath = file.relativeTo(xcframeworkDir.parentFile).path
                    val entry = ZipEntry(relativePath).apply {
                        time = reproducibleZipEntryTime
                    }
                    zos.putNextEntry(entry)
                    file.inputStream().buffered().use { input ->
                        input.copyTo(zos)
                    }
                    zos.closeEntry()
                }
        }

        // 2. Compute SHA-256
        println("Computing SHA-256 checksum...")
        val digest = MessageDigest.getInstance("SHA-256")
        FileInputStream(zipFile).use { fis ->
            val buffer = ByteArray(8192)
            var bytesRead = fis.read(buffer)
            while (bytesRead != -1) {
                digest.update(buffer, 0, bytesRead)
                bytesRead = fis.read(buffer)
            }
        }
        val checksumBytes = digest.digest()
        val checksum = checksumBytes.joinToString("") { "%02x".format(it) }
        println("SHA-256: $checksum")

        // 3. Write Package.swift
        println("Writing Package.swift to ${packageSwiftFile.absolutePath}...")
        packageSwiftFile.writeText(
            """
            // swift-tools-version:5.5
            import PackageDescription

            let package = Package(
                name: "OfdProtoCodec",
                platforms: [
                    .iOS(.v15)
                ],
                products: [
                    .library(
                        name: "OfdProtoCodec",
                        targets: ["OfdProtoCodec"]
                    ),
                ],
                dependencies: [],
                targets: [
                    .binaryTarget(
                        name: "OfdProtoCodec",
                        url: "$repoUrl/releases/download/v$versionStr/$zipName",
                        checksum: "$checksum"
                    )
                ]
            )
            """.trimIndent() + "\n"
        )
        println("SPM manifest generation complete for version $versionStr!")
    }
}
