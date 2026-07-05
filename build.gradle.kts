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
    alias(libs.plugins.nmcp)
    alias(libs.plugins.nmcp.aggregation)
    `maven-publish`
    signing
}

group = "io.github.texport"
version = "1.2.1"

repositories {
    mavenLocal()
    google()
    mavenCentral()
}

dependencies {
    detektPlugins(libs.detekt.formatting)
    add("nmcpAggregation", dependencies.project(mapOf("path" to ":")))
}

detekt {
    config.setFrom(files("$rootDir/config/detekt/detekt.yml"))
    buildUponDefaultConfig = true
    allRules = true
    autoCorrect = true
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

publishing {
    publications.withType<MavenPublication>().configureEach {
        val javadocJarTask = tasks.register<Jar>("${name}JavadocJar") {
            description = "Generates Javadoc jar for publication ${this@configureEach.name}"
            archiveClassifier.set("javadoc")
            archiveAppendix.set(this@configureEach.name)
        }
        artifact(javadocJarTask)
        pom {
            name.set("ofd-proto-codec")
            description.set("Trilingual CPCR/OFD protocol codec with provider/version modules")
            url.set("https://github.com/texport/ofd-proto-codec")

            licenses {
                license {
                    name.set("The Apache License, Version 2.0")
                    url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                }
            }

            developers {
                developer {
                    id.set("sergeyivanov")
                    name.set("Sergey Ivanov")
                    email.set("sergey.ivanov@example.com")
                }
            }

            scm {
                connection.set("scm:git:git://github.com/texport/ofd-proto-codec.git")
                developerConnection.set("scm:git:ssh://github.com/texport/ofd-proto-codec.git")
                url.set("https://github.com/texport/ofd-proto-codec")
            }
        }
    }
}

signing {
    val signingKey = System.getenv("SIGNING_KEY")
    val signingPassword = System.getenv("SIGNING_PASSWORD")
    if (!signingKey.isNullOrEmpty() && !signingPassword.isNullOrEmpty()) {
        useInMemoryPgpKeys(signingKey, signingPassword)
    }
    isRequired = false
    sign(publishing.publications)
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

nmcpAggregation {
    centralPortal {
        username.set(project.findProperty("ossrhUsername")?.toString() ?: System.getenv("OSSRH_USERNAME"))
        password.set(project.findProperty("ossrhPassword")?.toString() ?: System.getenv("OSSRH_PASSWORD"))
        publishingType.set("USER_MANAGED")
    }
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
