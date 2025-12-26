plugins {
    kotlin("jvm") version "2.2.0"
    kotlin("plugin.serialization") version "2.2.0"
}

group = "kz.mybrain"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("kz.kazakhtelecom:ofd-kt-proto-v203:2.0.3")
    implementation("com.google.protobuf:protobuf-java:3.25.3")
    testImplementation(kotlin("test"))
    testImplementation("kz.mybrain:ofd-network-client:1.0.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
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
