plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
rootProject.name = "ofd-proto-codec"



// Если рядом есть репозиторий ofd-network-client, подключаем его для тестов.
val networkClientRepo = file("../ofd-network-client")
if (networkClientRepo.exists()) {
    includeBuild("../ofd-network-client")
}
