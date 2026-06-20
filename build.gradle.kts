plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.detekt)
}

group = "kz.mybrain"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

detekt {
    config.setFrom(files("$rootDir/config/detekt/detekt.yml"))
    buildUponDefaultConfig = true
    allRules = true
}

dependencies {
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.ofd.kt.proto)
    implementation(libs.protobuf.java)
    testImplementation(kotlin("test"))
    testImplementation(libs.ofd.network.client)
    testImplementation(libs.kotlinx.coroutines.core)
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        showStandardStreams = true
    }
}

kotlin {
    jvmToolchain(17)
}

