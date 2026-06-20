# OFD Proto Codec

Библиотека на Kotlin/JVM для сериализации JSON‑объектов в `ByteArray` (header + payload)
и обратной десериализации `ByteArray` в JSON по протоколу ККМ → ОФД.

Сейчас реализован ОФД Казахтелеком, протокол 2.0.3 (v203).

## Основная идея

- На вход библиотеке дается JSON с `ofdId`, `protocolVersion`, `messageType`, `commandType`.
- На выходе `encode` возвращает JSON с `size` и `messageBase64`.
- `decode` принимает сырой `ByteArray` (header + payload) и возвращает JSON‑конверт.
- Валидаторы собирают **все** ошибки и возвращают их одним списком.
- Ошибки и сообщения — на русском и английском.

## Архитектура проекта

- `src/main/kotlin/kz/mybrain/ofdcodec/application`  
  Публичный фасад `OfdCodec`, регистрация протоколов.
- `src/main/kotlin/kz/mybrain/ofdcodec/domain`  
  Общие модели, порты, регистры, утилиты валидации.
- `src/main/kotlin/kz/mybrain/ofdcodec/infrastructure`  
  Работа с header, JSON‑маппингом, протокол‑версиями.
- `src/main/kotlin/kz/mybrain/ofdcodec/ofd/kazakhtelecom/v203`  
  Специфика ОФД Казахтелеком v203: билдеры, валидаторы, десериализаторы.

## Подключение proto библиотеки

Используется `ofd-kt-proto-v203` через composite build:

`settings.gradle.kts`:
```kotlin
includeBuild("../ofd-kt-proto")
```

`build.gradle.kts`:
```kotlin
dependencies {
    implementation("kz.mybrain:ofd-kt-proto-v203:2.0.3")
}
```

## Использование

Подробная документация по JSON-форматам: `docs/USAGE.md`.

### Сериализация (JSON → ByteArray)
```kotlin
val codec = OfdCodec(DefaultRegistry.create())
val result = codec.encode(jsonElement)
result.onSuccess { jsonOut ->
    val size = jsonOut["size"]
    val base64 = jsonOut["messageBase64"]
}
```

### Десериализация (ByteArray → JSON)
```kotlin
val codec = OfdCodec(DefaultRegistry.create())
val result = codec.decode(byteArray)
result.onSuccess { jsonOut ->
    // jsonOut — полный конверт ответа
}
```

## Запуск тестов

### Обычные тесты (без сети)
```bash
./gradlew test --rerun-tasks --console=plain
```

### Интеграционный тест с TCP‑клиентом

Если рядом есть `../ofd-network-client`, он подключается через composite build.

Переменные окружения:
```bash
OFD_TEST_HOST=37.150.215.187
OFD_TEST_PORT=7777
OFD_TEST_TOKEN=...        # актуальный токен, выданный сервером
OFD_TEST_REQNUM_BASE=1000 # базовый reqNum, чтобы тесты не конфликтовали
```

Во время прогона сетевых тестов последний токен сохраняется в
`build/tmp/ofd-test-token.txt` и используется для следующих запросов.
Если файл отсутствует или пуст, `OFD_TEST_TOKEN` используется как стартовый токен и
записывается в файл. Чтобы принудительно задать новый токен, удалите файл или очистите его.

Запуск:
```bash
OFD_TEST_HOST=37.150.215.187 OFD_TEST_PORT=7777 OFD_TEST_TOKEN=... OFD_TEST_REQNUM_BASE=1000 \
./gradlew test --tests CommandSystemNetworkClientTest --rerun-tasks --console=plain
```

В выводе будут:
- полный JSON запроса,
- base64 ответа,
- raw proto‑ответ от сервера,
- JSON после десериализации.

## Добавление новой версии протокола

Общая идея:
- создать новый модуль с другим package и artifactId;
- зарегистрировать новый handler в `DefaultRegistry`;
- добавить валидаторы и сериализаторы для новой версии.
