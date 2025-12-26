plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}
rootProject.name = "ofd-proto-codec"

// Если рядом есть репозиторий ofd-kt-proto, подключаем его как composite build.
val protoRepo = file("../ofd-kt-proto")
if (protoRepo.exists()) {
    includeBuild("../ofd-kt-proto")
}

// Если рядом есть репозиторий ofd-network-client, подключаем его для тестов.
val networkClientRepo = file("../ofd-network-client")
if (networkClientRepo.exists()) {
    includeBuild("../ofd-network-client")
}
