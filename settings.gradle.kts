pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

/**
 * Версия из каталога `gradle/libs.versions.toml` — до того, как Gradle его прочтёт.
 */
fun catalogVersion(key: String): String =
    file("gradle/libs.versions.toml").readLines().firstNotNullOf { line ->
        Regex("""^$key\s*=\s*"(.+)"\s*$""").find(line)?.groupValues?.get(1)
    }

/**
 * Готовая сборка соседней библиотеки texport из её выпуска на GitHub.
 *
 * Выпуск несёт архив `<архив>-maven-<версия>.zip` — собранную библиотеку
 * для всех целей KMP с метаданными Gradle. Он раскладывается в локальный
 * Maven один раз на версию; дальше сборка берёт библиотеку оттуда
 * (`mavenLocal()` в хранилищах стоит первым). Собирать соседей из исходников
 * не нужно ни на GitHub, ни у разработчика, а в Maven Central их нет.
 * Версию `-SNAPSHOT` не скачивают: это своя локальная сборка библиотеки.
 *
 * @param repository репозиторий texport с выпусками.
 * @param archive имя архива в выпуске: в одном репозитории их может быть
 * несколько (протоколы 2.0.3 и 2.0.4 выпускаются порознь).
 * @param artifact артефакт, по которому видно, что версия уже разложена.
 * @param version версия выпуска без `v`.
 */
fun releasedLibrary(repository: String, archive: String, artifact: String, version: String) {
    if (version.endsWith("-SNAPSHOT")) return
    val local = File(System.getProperty("user.home"), ".m2/repository").canonicalFile
    if (File(local, "io/github/texport/$artifact/$version").isDirectory) return
    val address = "https://github.com/texport/$repository/releases/download/v$version/$archive-maven-$version.zip"
    val unpacked = File(local, ".$archive-$version-unpacking").apply { deleteRecursively(); mkdirs() }
    java.util.zip.ZipInputStream(uri(address).toURL().openStream().buffered()).use { zip ->
        generateSequence { zip.nextEntry }.filterNot { it.isDirectory }.forEach { entry ->
            val target = File(unpacked, entry.name).canonicalFile
            require(target.startsWith(unpacked)) { "Файл вне архива: ${entry.name}" }
            target.parentFile.mkdirs()
            target.outputStream().use { zip.copyTo(it) }
        }
    }
    // Уже лежащее не трогается: список версий артефакта в локальном Maven
    // знает и свои сборки, а архив — только свою версию.
    unpacked.copyRecursively(local, overwrite = false) { _, _ -> OnErrorAction.SKIP }
    unpacked.deleteRecursively()
}

// Протоколы 2.0.3 и 2.0.4 живут в одном репозитории, но выпускаются порознь,
// каждый своим архивом и своей версией.
releasedLibrary("ofd-kt-proto", "ofd-kt-proto", "ofd-kt-proto", catalogVersion("ofdKtProto"))
releasedLibrary("ofd-kt-proto", "ofd-kt-proto-v204", "ofd-kt-proto-v204", catalogVersion("ofdKtProtoV204"))
releasedLibrary("ofd-network-client", "ofd-network-client", "ofd-network-client", catalogVersion("ofdNetworkClient"))

rootProject.name = "ofd-proto-codec"
