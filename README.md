# ofd-proto-codec

[![Maven Central](https://img.shields.io/maven-central/v/io.github.texport/ofd-proto-codec.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/io.github.texport/ofd-proto-codec)
[![Version](https://img.shields.io/badge/version-1.2.0-blue.svg)](https://github.com/texport/ofd-proto-codec/releases)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![CI Build](https://img.shields.io/github/actions/workflow/status/texport/ofd-proto-codec/ci.yml?branch=main&label=CI%20Build)](https://github.com/texport/ofd-proto-codec/actions)
[![Coverage](https://img.shields.io/badge/coverage-98%25-brightgreen.svg)](https://github.com/texport/ofd-proto-codec/actions)

---

### [Documentation in English](#documentation-in-english) &middot; [Документация на русском языке](#документация-на-русском-языке) &middot; [Usage](docs/USAGE.md) &middot; [Extending Guide](docs/EXTENDING.md) &middot; [Release Checklist](docs/RELEASING.md)

---

> [!IMPORTANT]
> **Disclaimer:** This is an unofficial, community-maintained library. It is not officially endorsed by, affiliated with, or sponsored by JSC "KazakhTelecom", the State Revenue Committee of the Republic of Kazakhstan, or any official OFD provider.
>
> **Дисклеймер:** Данный проект является неофициальной библиотекой, поддерживаемой сообществом. Он не связан, не спонсируется и не утверждался АО «Казахтелеком», Комитетом государственных доходов РК или любыми другими официальными провайдерами ОФД.

---

## Documentation in English

A lightweight, robust, and clean-architecture Kotlin Multiplatform (KMP) library for serializing JSON objects into raw CPCR/OFD protocol format (header + payload) and deserializing raw byte arrays back to structured JSON according to provider-specific KKM-to-OFD protocol modules.

The library is provider/version oriented. At the moment, the only implemented provider module is KazakhTelecom OFD protocol version 2.0.3 (v203); future OFD providers or protocol versions should be added as separate modules without changing the core codec facade.

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
                implementation("io.github.texport:ofd-proto-codec:1.2.0")
            }
        }
    }
}
```

#### Apple Swift Package Manager (SPM)
You can integrate this library directly into your iOS project using Xcode's Swift Package Manager:
1. In Xcode, select **File ➔ Add Package Dependencies...**
2. Enter the repository URL: `https://github.com/texport/ofd-proto-codec.git`
3. Set the version rules to **Up to Next Major** starting with `1.2.0`.

### Release 1.2.0
- Adds the Android KMP target and Android AAR publication.
- Updates test integration dependency `ofd-network-client` to `1.2.0`.
- Hardens JSON envelope validation for `header.reqNum` (`0..65535`).
- Documents the provider/version extension policy and keeps `kazakhtelecom/v203` stable.

---

## Документация на русском языке

Легковесная библиотека на Kotlin Multiplatform (KMP) для сериализации JSON-объектов в сырой формат протокола CPCR/OFD (заголовок + полезная нагрузка) и обратной десериализации байтовых массивов в структурированный JSON через provider-specific модули протокола обмена ККМ → ОФД.

Библиотека спроектирована вокруг отдельных модулей ОФД/версий протокола. На данный момент реализован только модуль протокола Казахтелеком версии 2.0.3 (v203); новые ОФД или версии протокола должны добавляться отдельными модулями без изменения ядра кодека.

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
                implementation("io.github.texport:ofd-proto-codec:1.2.0")
            }
        }
    }
}
```

#### В Apple iOS проектах (через SPM)
Вы можете подключить библиотеку непосредственно в iOS приложение с помощью Swift Package Manager в Xcode:
1. Выберите в Xcode: **File ➔ Add Package Dependencies...**
2. Введите URL репозитория: `https://github.com/texport/ofd-proto-codec.git`
3. Установите правило версии **Up to Next Major** начиная с `1.2.0`.

### Релиз 1.2.0
- Добавлена Android KMP target и публикация Android AAR.
- Тестовая интеграционная зависимость `ofd-network-client` обновлена до `1.2.0`.
- Усилена валидация JSON-конверта для `header.reqNum` (`0..65535`).
- Зафиксирована политика расширения через отдельные provider/version модули; `kazakhtelecom/v203` остается стабильным.
