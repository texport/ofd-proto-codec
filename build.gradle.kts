plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.detekt)
    alias(libs.plugins.nmcp)
    id("maven-publish")
    id("signing")
}


group = "io.github.texport"
version = "1.0.0"

repositories {
    mavenLocal()
    mavenCentral()
}

detekt {
    config.setFrom(files("$rootDir/config/detekt/detekt.yml"))
    buildUponDefaultConfig = true
    allRules = true
    autoCorrect = true
}

dependencies {
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.ofd.kt.proto)
    implementation(libs.protobuf.java)
    testImplementation(kotlin("test"))
    testImplementation(libs.ofd.network.client)
    testImplementation(libs.kotlinx.coroutines.core)
    detektPlugins(libs.detekt.formatting)
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

val sourcesJar = tasks.register<Jar>("sourcesJar") {
    archiveClassifier.set("sources")
    from(sourceSets.main.get().allSource)
}

val javadocJar = tasks.register<Jar>("javadocJar") {
    archiveClassifier.set("javadoc")
    from(tasks.javadoc)
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            artifact(sourcesJar)
            artifact(javadocJar)

            pom {
                name.set("ofd-proto-codec")
                description.set("Trilingual protocol codec for Kazakh OFD Protocol 2.0.3")
                url.set("https://github.com/texport/superkassa")

                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
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
                    connection.set("scm:git:git://github.com/texport/superkassa.git")
                    developerConnection.set("scm:git:ssh://github.com/texport/superkassa.git")
                    url.set("https://github.com/texport/superkassa")
                }
            }
        }
    }
}


signing {
    val signingKey = System.getenv("SIGNING_KEY")
    val signingPassword = System.getenv("SIGNING_PASSWORD")
    if (!signingKey.isNullOrEmpty() && !signingPassword.isNullOrEmpty()) {
        useInMemoryPgpKeys(signingKey, signingPassword)
        sign(publishing.publications["mavenJava"])
    }
}

nmcp {
    publishAllPublicationsToCentralPortal {
        username.set(project.findProperty("ossrhUsername")?.toString() ?: System.getenv("OSSRH_USERNAME"))
        password.set(project.findProperty("ossrhPassword")?.toString() ?: System.getenv("OSSRH_PASSWORD"))
        publishingType.set("USER_MANAGED")
    }
}


