# ofd-proto-codec

[![Maven Central](https://img.shields.io/maven-central/v/io.github.texport/ofd-proto-codec.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/io.github.texport/ofd-proto-codec)
[![Version](https://img.shields.io/badge/version-1.0.1-blue.svg)](https://github.com/texport/ofd-proto-codec/releases)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![CI Build](https://img.shields.io/github/actions/workflow/status/texport/ofd-proto-codec/ci.yml?branch=main&label=CI%20Build)](https://github.com/texport/ofd-proto-codec/actions)

---

### [Documentation in English](#documentation-in-english) &middot; [Документация на русском языке](#документация-на-русском-языке) &middot; [Extending Guide](docs/EXTENDING.md)

---

> [!IMPORTANT]
> **Disclaimer:** This is an unofficial, community-maintained library. It is not officially endorsed by, affiliated with, or sponsored by JSC "KazakhTelecom", the State Revenue Committee of the Republic of Kazakhstan, or any official OFD provider.
>
> **Дисклеймер:** Данный проект является неофициальной библиотекой, поддерживаемой сообществом. Он не связан, не спонсируется и не утверждался АО «Казахтелеком», Комитетом государственных доходов РК или любыми другими официальными провайдерами ОФД.

---

## Documentation in English

A lightweight, robust, and clean-architecture Kotlin/JVM library for serializing JSON objects into raw CPCR protocol format (header + payload) and deserializing raw byte arrays back to structured JSON according to the Kazakh KKM-to-OFD protocol.

Currently, it fully implements KazakhTelecom OFD protocol version 2.0.3 (v203).

### Key Features
- **Trilingual Localized Error Messages**: All validation errors are gathered in a single pass and translated into Russian, Kazakh, and English (`RU: ... | KK: ... | EN: ...`).
- **Complete Offline Validation**: Automatically validates types, bounds, mandatory blocks, and constraints before attempting binary serialization.
- **Centralized JSON Mapper**: Converts standard JSON envelopes directly to schema-compliant Protobuf byte payloads.
- **Clean Architecture**: Decoupled from transport code, serving solely as a presentation-layer codec.

---

### Architecture & Design Principles

The library is built strictly following modern software design principles:
- **Clean Architecture (Presentation Layer)**: The codec acts as the presentation layer. It is decoupled from low-level transport (handled by `ofd-network-client`) and low-level proto classes (handled by `ofd-kt-proto`), dealing strictly with encoding JSON to bytes and decoding bytes to JSON.
- **KISS (Keep It Simple, Stupid)**: Features a single-pass validator that scans the entire request/response structure and returns all validation errors at once instead of failing on the first error, simplifying client-side error processing.
- **SOLID**:
  - *Single Responsibility (SRP)*: Validation, serialization, deserialization, and registry of codec versions are separated into individual classes.
  - *Open/Closed (OCP) & Dependency Inversion (DIP)*: Easily expandable for other OFD providers and protocols by registering new module serializers/deserializers in the registry. See [Extending Guide](docs/EXTENDING.md).
  - *Interface Segregation (ISP)*: Uses dedicated interfaces for validation, request serialization, and response deserialization.

---

### Exception & Error Message Model

For ease of logging and operations in bilingual or multilingual environments, the library provides a trilingual validation error model:
- **Trilingual Errors**: Validation exceptions contain error messages in Russian, Kazakh, and English (`messageRu`, `messageKk`, `messageEn`). This allows rendering errors to cashiers/support in Russian or Kazakh, and developers in English without extra translation layers.
- **Exceptions**:
  - `OfdCodecException`: Thrown when validation errors or serialization/deserialization failures occur, holding a detailed list of `ValidationError` objects.

---

### Installation

The library is officially published and hosted on **Maven Central**.

#### Via Maven Central (Recommended)
Add the dependency to your `build.gradle.kts`:

```kotlin
dependencies {
    implementation("io.github.texport:ofd-proto-codec:1.0.1")
}
```

> [!TIP]
> **Local Development (Optional):** If you are contributing to the library itself and want to test changes locally from source, you can include the local directory as a Composite Build in your consumer's `settings.gradle.kts` via `includeBuild("../ofd-proto-codec")`.

---

### Usage Example

Detailed documentation on JSON formats can be found in `docs/USAGE.md`.

#### Serialization (JSON → ByteArray)
```kotlin
import kz.mybrain.ofdcodec.application.OfdCodec
import kz.mybrain.ofdcodec.application.DefaultRegistry
import kotlinx.serialization.json.JsonElement

val registry = DefaultRegistry.create()
val codec = OfdCodec(registry)
val result = codec.encode(jsonElement)

result.onSuccess { jsonOut ->
    val size = jsonOut["size"]
    val base64 = jsonOut["messageBase64"]
    // Ready to be sent over TCP socket
}.onFailure { throwable ->
    // Prints trilingual errors
    System.err.println(throwable.message)
}
```

#### Deserialization (ByteArray → JSON)
```kotlin
val registry = DefaultRegistry.create()
val codec = OfdCodec(registry)
val result = codec.decode(byteArray)

result.onSuccess { jsonEnvelope ->
    val payload = jsonEnvelope["payload"]
    // Process response payload
}
```

---

## Документация на русском языке

Легковесная библиотека на Kotlin/JVM для сериализации JSON-объектов в сырой формат протокола CPCR (заголовок + полезная нагрузка) и обратной десериализации байтовых массивов в структурированный JSON согласно государственному стандарту протокола обмена ККМ → ОФД Республики Казахстан.

В настоящее время полностью реализован протокол Казахтелеком версии 2.0.3 (v203).

### Преимущества
- **Трехъязычная локализация ошибок**: Все ошибки валидации собираются за один проход и выводятся на русском, казахском и английском языках (`RU: ... | KK: ... | EN: ...`).
- **Полная автономная валидация**: Перед сериализацией автоматически проверяются типы полей, обязательные блоки и диапазоны значений согласно требованиям спецификации.
- **Чистая архитектура**: Библиотека абстрагирована от сетевого транспорта (TCP/HTTP) и занимается исключительно кодированием и декодированием данных.

---

### Архитектура и принципы проектирования

Проект разработан в строгом соответствии с ключевыми инженерными практиками:
- **Clean Architecture (Слой представления)**: Кодек выступает в качестве слоя представления. Он полностью отделен от низкоуровневой сетевой части (которую выполняет `ofd-network-client`) и от сгенерированных protobuf-классов (которыми занимается `ofd-kt-proto`), обеспечивая чистый API перевода JSON в байты и обратно.
- **KISS (Keep It Simple, Stupid)**: Однопроходная валидация собирает весь список ошибок за раз, позволяя отобразить пользователю все недочеты заполнения формы чека одновременно.
- **SOLID**:
  - *Single Responsibility (SRP)*: Логика валидации, сериализации и десериализации разделена на независимые классы.
  - *Open/Closed (OCP) & Dependency Inversion (DIP)*: Расширение поддержки новых ОФД и версий протокола реализуется через регистрацию модулей в реестре без изменения ядра кодека. Подробнее в [Руководстве по расширению](docs/EXTENDING.md).
  - *Interface Segregation (ISP)*: Использование специализированных интерфейсов для валидаторов и сериализаторов.

---

### Модель ошибок и исключений

Для удобства логирования и эксплуатации в многоязычной среде (Казахстан), библиотека поддерживает генерацию сообщений об ошибках сразу на трех языках:
- **Трехъязычные ошибки**: Каждая ошибка `ValidationError` содержит переводы на русский (`messageRu`), казахский (`messageKk`) и английский (`messageEn`) языки.
- **Исключения**:
  - `OfdCodecException`: Вызывается при ошибках валидации или сбоях кодирования, предоставляя полный список `ValidationError`.

---

### Подключение библиотеки

Библиотека официально опубликована и доступна в репозитории **Maven Central**.

#### Через Maven Central (Рекомендуемый способ)
Добавьте зависимость в ваш `build.gradle.kts`:

```kotlin
dependencies {
    implementation("io.github.texport:ofd-proto-codec:1.0.1")
}
```

> [!TIP]
> **Локальная разработка (Опционально):** Если вы дорабатываете саму библиотеку и хотите тестировать изменения локально из исходников, вы можете временно подключить её как Composite Build в `settings.gradle.kts` вашего основного проекта с помощью `includeBuild("../ofd-proto-codec")`.

---

### Пример использования

Подробное описание JSON-структур доступно в документе `docs/USAGE.md`.

#### Сериализация (JSON → ByteArray)
```kotlin
import kz.mybrain.ofdcodec.application.OfdCodec
import kz.mybrain.ofdcodec.application.DefaultRegistry
import kotlinx.serialization.json.JsonElement

val registry = DefaultRegistry.create()
val codec = OfdCodec(registry)
val result = codec.encode(jsonElement)

result.onSuccess { jsonOut ->
    val size = jsonOut["size"]
    val base64 = jsonOut["messageBase64"]
    // Готово к отправке в TCP сокет
}.onFailure { throwable ->
    // Выводит сообщения об ошибках на трех языках
    System.err.println(throwable.message)
}
```

#### Десериализация (ByteArray → JSON)
```kotlin
val registry = DefaultRegistry.create()
val codec = OfdCodec(registry)
val result = codec.decode(byteArray)

result.onSuccess { jsonEnvelope ->
    val payload = jsonEnvelope["payload"]
    // Обрабатываем ответ сервера
}
```
