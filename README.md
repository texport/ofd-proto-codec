# ofd-proto-codec

[![Maven Central](https://img.shields.io/maven-central/v/io.github.texport/ofd-proto-codec.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/io.github.texport/ofd-proto-codec)
[![Version](https://img.shields.io/badge/version-1.2.1-blue.svg)](https://github.com/texport/ofd-proto-codec/releases)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![CI Build](https://img.shields.io/github/actions/workflow/status/texport/ofd-proto-codec/ci.yml?branch=main&label=CI%20Build)](https://github.com/texport/ofd-proto-codec/actions)
[![Coverage](https://img.shields.io/badge/coverage-98%25-brightgreen.svg)](https://github.com/texport/ofd-proto-codec/actions)

---

### [Documentation in English](#documentation-in-english) &middot; [Документация на русском языке](#документация-на-русском-языке) &middot; [Usage](docs/USAGE.md) &middot; [Extending Guide](docs/EXTENDING.md)

---

> [!IMPORTANT]
> **Disclaimer:** This is an unofficial, community-maintained library. It is not officially endorsed by, affiliated with, or sponsored by the State Revenue Committee of the Republic of Kazakhstan or any official OFD provider.
>
> **Дисклеймер:** Данный проект является неофициальной библиотекой, поддерживаемой сообществом. Он не связан, не спонсируется и не утверждался Комитетом государственных доходов РК или любыми официальными провайдерами ОФД.

---

## Documentation in English

A lightweight, robust, and clean-architecture Kotlin Multiplatform (KMP) library for serializing JSON objects into raw CPCR/OFD protocol format (header + payload) and deserializing raw byte arrays back to structured JSON according to provider-specific KKM-to-OFD protocol modules.

The library is provider/version oriented. At the moment, the only implemented provider module is `kazakhtelecom/v203`; future OFD providers or protocol versions should be added as separate modules without changing the core codec facade.

### Supported Matrix
- **Targets**: JVM, Android, iOS device, and iOS simulator.
- **Currently supported provider module**: `kazakhtelecom` protocol `203`.
- **Transport boundary**: TCP/HTTP exchange is intentionally outside this library; use a network client in your application or integration tests.

### Key Features
- **Kotlin Multiplatform Support**: Runs on JVM, Android, and Apple/iOS Native platforms.
- **Trilingual Localized Error Messages**: All validation errors are gathered in a single pass and translated into Russian, Kazakh, and English (`RU: ... | KK: ... | EN: ...`).
- **Complete Offline Validation**: Automatically validates types, bounds, mandatory blocks, and constraints before attempting binary serialization.
- **Centralized JSON Mapper**: Converts standard JSON envelopes directly to schema-compliant Protobuf byte payloads.

### Installation

#### Kotlin Multiplatform & Android
Add the dependency to your shared `commonMain` source set inside `build.gradle.kts`:

```kotlin
kotlin {
    sourceSets {
        commonMain {
            dependencies {
                implementation("io.github.texport:ofd-proto-codec:1.2.1")
            }
        }
    }
}
```

#### Apple Swift Package Manager (SPM)
You can integrate this library directly into your iOS project using Xcode's Swift Package Manager:
1. In Xcode, select **File ➔ Add Package Dependencies...**
2. Enter the repository URL: `https://github.com/texport/ofd-proto-codec.git`
3. Set the version rules to **Up to Next Major** starting with `1.2.1`.

### Quick Start / Usage

Here is how to initialize the codec and use it to encode a JSON request or decode an OFD binary response:

```kotlin
import kz.mybrain.ofdcodec.application.DefaultRegistry
import kz.mybrain.ofdcodec.application.OfdCodec
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.JsonPrimitive

// 1. Initialize the registry and codec facade
val registry = DefaultRegistry.create()
val codec = OfdCodec(registry)

// 2. Encode a JSON request envelope into raw bytes
val requestEnvelope = buildJsonObject {
    put("ofdId", "kazakhtelecom")
    put("protocolVersion", "203")
    put("messageType", "REQUEST")
    put("commandType", "COMMAND_SYSTEM")
    put("header", buildJsonObject {
        put("appCode", 1)
        put("deviceId", 12345L)
        put("token", 99999L)
        put("reqNum", 42)
    })
    put("payload", buildJsonObject {
        put("service", buildJsonObject {
            put("getRegInfo", true)
            put("offlinePeriod", buildJsonObject {
                put("beginTime", buildJsonObject {
                    put("date", buildJsonObject { put("year", 2026); put("month", 7); put("day", 5) })
                    put("time", buildJsonObject { put("hour", 22); put("minute", 0); put("second", 0) })
                })
                put("endTime", buildJsonObject {
                    put("date", buildJsonObject { put("year", 2026); put("month", 7); put("day", 5) })
                    put("time", buildJsonObject { put("hour", 22); put("minute", 0); put("second", 0) })
                })
            })
            put("securityStats", buildJsonObject {
                put("ticketAdSentCount", 0)
                put("ticketAdFailedCount", 0)
            })
            put("regInfo", buildJsonObject {
                put("kkm", buildJsonObject {
                    put("fnsKkmId", "123")
                    put("serialNumber", "456")
                    put("kkmId", "789")
                })
                put("org", buildJsonObject {
                    put("title", "My Org")
                    put("address", "Address")
                    put("inn", "123456789012")
                    put("addressKz", "Address KZ")
                })
            })
        })
    })
}

val encodeResult = codec.encode(requestEnvelope)
if (encodeResult.isSuccess) {
    val resultJson = encodeResult.getOrThrow()
    val size = resultJson["size"]
    val messageBase64 = resultJson["messageBase64"]
    println("Encoded size: $size")
} else {
    val exception = encodeResult.exceptionOrNull()
    println("Validation failed: ${exception?.message}")
}

// 3. Decode an OFD binary response packet
val rawResponseBytes: ByteArray = ByteArray(0) // received from OFD server
val decodeResult = codec.decode(rawResponseBytes)
if (decodeResult.isSuccess) {
    val responseEnvelope = decodeResult.getOrThrow()
    println("Decoded envelope: $responseEnvelope")
}
```

### Architecture Boundary

The library follows clean architecture principles:
- **Core Facade (`OfdCodec`):** Exposes `encode` and `decode` endpoints, orchestrating header construction/parsing, handler lookup, and validation.
- **Provider Modules (`OfdProtocolHandler`):** Plug-in modules implementing serialization, deserialization, and validation for specific OFD providers.
- **Delegation to Ports:** Network transport, TCP connection handling, TLS handshake, HTTP encapsulation, or offline queue queuing are completely delegated to consumer applications or integration libraries (e.g. `ofd-network-client`).

---

## Документация на русском языке

Легковесная библиотека на Kotlin Multiplatform (KMP) для сериализации JSON-объектов в сырой формат протокола CPCR/OFD (заголовок + полезная нагрузка) и обратной десериализации байтовых массивов в структурированный JSON через provider-specific модули протокола обмена ККМ → ОФД.

Библиотека спроектирована вокруг отдельных модулей ОФД/версий протокола. На данный момент реализован только модуль `kazakhtelecom/v203`; новые ОФД или версии протокола должны добавляться отдельными модулями без изменения ядра кодека.

### Матрица поддержки
- **Платформы**: JVM, Android, iOS device и iOS simulator.
- **Текущий поддерживаемый модуль ОФД**: `kazakhtelecom`, протокол `203`.
- **Граница транспорта**: TCP/HTTP обмен намеренно находится вне этой библиотеки; сетевой клиент подключается в приложении или интеграционных тестах.

### Преимущества
- **Поддержка Kotlin Multiplatform**: Работает на JVM, Android и нативных Apple/iOS платформах.
- **Трехъязычная локализация ошибок**: Все ошибки валидации собираются за один проход и выводятся на русском, казахском и английском языках (`RU: ... | KK: ... | EN: ...`).
- **Полная автономная валидация**: Перед сериализацией автоматически проверяются типы полей, обязательные блоки и диапазоны значений согласно требованиям спецификации.
- **Чистая архитектура**: Библиотека абстрагирована от сетевого транспорта (TCP/HTTP) и занимается исключительно кодированием и декодированием данных.

### Подключение библиотеки

#### В Kotlin Multiplatform и Android
Добавьте зависимость в ваш общий набор исходников `commonMain` в `build.gradle.kts`:

```kotlin
kotlin {
    sourceSets {
        commonMain {
            dependencies {
                implementation("io.github.texport:ofd-proto-codec:1.2.1")
            }
        }
    }
}
```

#### В Apple iOS проектах (через SPM)
Вы можете подключить библиотеку непосредственно в iOS приложение с помощью Swift Package Manager в Xcode:
1. Выберите в Xcode: **File ➔ Add Package Dependencies...**
2. Введите URL репозитория: `https://github.com/texport/ofd-proto-codec.git`
3. Установите правило версии **Up to Next Major** начиная с `1.2.1`.
