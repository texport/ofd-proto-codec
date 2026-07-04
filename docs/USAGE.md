# OFD Proto Codec: JSON API / Спецификация JSON API

This document describes the JSON formats for encode (REQUEST) and decode (RESPONSE). All validation errors are returned as a list and contain messages in Russian, Kazakh, and English.
/ Этот документ описывает JSON‑форматы для encode (REQUEST) и decode (RESPONSE). Все ошибки возвращаются сразу списком и содержат сообщения на русском, казахском и английском языках.

## Common JSON Envelope / Общий JSON‑конверт

### Request (encode) / Запрос (encode)
```json
{
  "ofdId": "kazakhtelecom",
  "protocolVersion": "203",
  "messageType": "REQUEST",
  "commandType": "COMMAND_SYSTEM",
  "header": {
    "deviceId": 201873,
    "token": 2376246852,
    "reqNum": 1
  },
  "payload": { ... }
}
```

### Response (decode) / Ответ (decode)
```json
{
  "ofdId": "kazakhtelecom",
  "protocolVersion": "203",
  "messageType": "RESPONSE",
  "commandType": "COMMAND_REPORT",
  "header": {
    "size": 321,
    "deviceId": 201873,
    "token": 2376246852,
    "reqNum": 1
  },
  "payload": { ... }
}
```

### Result (common response block) / Result (общий блок ответа)
```json
{
  "result": {
    "resultCode": 0,
    "resultText": "OK",
    "resultType": {
      "code": 0,
      "name": "RESULT_TYPE_OK",
      "descriptionRu": "Команда выполнена успешно",
      "descriptionEn": "Command completed successfully"
    }
  }
}
```

### Important Rules / Важные правила
- `protocolVersion` is in the `"203"` format, without dots. / `protocolVersion` в формате `"203"`, без точек.
- `messageType` is always `REQUEST` for encode and `RESPONSE` for decode. / `messageType` всегда `REQUEST` для encode и `RESPONSE` для decode.
- `header.size` and `messageBase64` are returned only from `encode`. / `header.size` и `messageBase64` возвращаются только из `encode`.
- `appCode` is **not passed** in JSON and is **not returned** in JSON. / `appCode` **не передается** в JSON и **не возвращается** в JSON.
- `header.reqNum` must fit the unsigned 16-bit request number range `0..65535`. / `header.reqNum` должен находиться в диапазоне unsigned 16-bit номера запроса `0..65535`.

## Token Rotation / Токен и его смена

The token is rotated in the server's response. The new token is taken from `header.token` of the response and must be used in the subsequent request.
/ Токен меняется в ответе сервера. Новый токен берется из `header.token` ответа и используется в следующем запросе.

Known commands that **rotate the token**: / Из известных команд, которые **меняют токен**:
- `COMMAND_TICKET`
- `COMMAND_CLOSE_SHIFT`

If other commands with token rotation are introduced, they should be added here. / Если появятся другие команды с ротацией токена, их нужно добавить в этот список.

## Validation Rules / Правила валидации

The library validates **all** fields and returns **all** found errors as a single list.
To avoid validation errors:
- satisfy all mandatory fields,
- do not mismatch types (e.g. `string` instead of `number` and vice versa),
- follow the numeric ranges (`uint32`, `uint64`) specified in the protocol.

/ Библиотека валидирует **все** поля и возвращает **все** найденные ошибки одним списком.
Чтобы избежать ошибок:
- соблюдайте обязательные поля,
- не путайте типы (`string` вместо `number` и наоборот),
- следуйте диапазонам (`uint32`, `uint64`) из протокола.

## Supported Commands (KazakhTelecom v203) / Поддерживаемые команды (KazakhTelecom v203)

- `COMMAND_AUTH`
- `COMMAND_SYSTEM`
- `COMMAND_INFO`
- `COMMAND_MONEY_PLACEMENT`
- `COMMAND_NOMENCLATURE`
- `COMMAND_REPORT`
- `COMMAND_TICKET`
- `COMMAND_CLOSE_SHIFT`

## Supported Targets / Поддерживаемые платформы

The published KMP library is intended for JVM, Android, and Apple/iOS targets. Android support is a real Gradle target and is published as an Android artifact; no Android-specific production API is required because codec behavior stays in `commonMain`.
/ Опубликованная KMP-библиотека предназначена для JVM, Android и Apple/iOS. Android поддерживается как полноценная Gradle target и публикуется как Android artifact; отдельный Android production API не нужен, потому что логика кодека остается в `commonMain`.

Network exchange is not part of this library. Use `ofd-network-client` or another transport implementation outside the codec boundary.
/ Сетевой обмен не входит в ответственность этой библиотеки. Используйте `ofd-network-client` или другую транспортную реализацию вне границ кодека.

## Payload: Command-specific structures / Payload: структура по командам

Below are the **maximum possible** JSON structures. Any fields marked as `optional` can be omitted.
/ Ниже приведены **максимально возможные** JSON‑структуры. Любые поля, отмеченные как `optional`, можно не передавать.

### COMMAND_AUTH (Request)
```json
{
  "service": { ... },
  "auth": {
    "login": "my_login",
    "password": "my_password"
  }
}
```

### COMMAND_AUTH (Response)
```json
{
  "commandType": "COMMAND_AUTH",
  "result": { ... },
  "auth": {
    "result": "RESULT_TYPE_OK",
    "operatorCode": "123",
    "operatorName": "Иван Иванов",
    "roles": [
      "USER_ROLE_ADMINISTRATOR",
      "USER_ROLE_PAYMASTER"
    ]
  }
}
```

### COMMAND_SYSTEM (Request)
```json
{
  "service": { ... } 
}
```

### COMMAND_SYSTEM (Response)
```json
{
  "commandType": "COMMAND_SYSTEM",
  "result": { ... },
  "service": { ... }
}
```

### COMMAND_INFO (Request)
```json
{
  "service": { ... }
}
```

### COMMAND_INFO (Response)
```json
{
  "commandType": "COMMAND_INFO",
  "result": { ... },
  "service": { ... }
}
```

### COMMAND_MONEY_PLACEMENT (Request)
```json
{
  "service": { ... },
  "moneyPlacement": {
    "dateTime": { "date": { "year": 2024, "month": 9, "day": 1 }, "time": { "hour": 11, "minute": 0, "second": 0 } },
    "operation": "MONEY_PLACEMENT_DEPOSIT",
    "sum": { "bills": 1000, "coins": 0 },
    "isOffline": false,
    "frShiftNumber": 1,
    "printedDocumentNumber": 123456,
    "operator": { "code": 1, "name": "Operator 1" }
  }
}
```

### COMMAND_MONEY_PLACEMENT (Response)
```json
{
  "commandType": "COMMAND_MONEY_PLACEMENT",
  "result": { ... },
  "service": { ... }
}
```

### COMMAND_NOMENCLATURE (Request)
```json
{
  "service": { ... },
  "nomenclature": {
    "currentVersion": 0,
    "barcode": "0200091530572"
  }
}
```

At least one of `currentVersion` or `barcode` must be provided. / `currentVersion` и `barcode` — хотя бы одно поле должно быть задано.

### COMMAND_NOMENCLATURE (Response)
```json
{
  "commandType": "COMMAND_NOMENCLATURE",
  "result": { ... },
  "service": { ... },
  "nomenclature": {
    "version": 1,
    "createdTime": { "date": { "year": 2024, "month": 9, "day": 1 }, "time": { "hour": 10, "minute": 0, "second": 0 } },
    "elements": [
      {
        "type": "ELEMENT_ITEM",
        "title": "Coca‑Cola 0.5",
        "titleKk": "Coca‑Cola 0.5",
        "parentGroupId": 1,
        "id": 100,
        "item": {
          "article": "ART‑001",
          "barcode": "0200091530572",
          "description": "Газированный напиток",
          "purchasePrice": { "bills": 100, "coins": 0 },
          "sellPrice": { "bills": 150, "coins": 0 },
          "discountPercent": 0,
          "discountSum": { "bills": 0, "coins": 0 },
          "markupPercent": 0,
          "markupSum": { "bills": 0, "coins": 0 },
          "taxes": [ { "taxationType": 0, "taxType": 1, "taxPercent": 1200 } ],
          "measureCount": 1,
          "measureTitle": "шт",
          "measureFractional": false,
          "measureUnitCode": "C62",
          "ntin": "12345678901234",
          "isMarkedeac": false,
          "isSocial": false
        }
      }
    ],
    "result": { "code": 0, "name": "NOMENCLATURE_RESULT_OK" }
  }
}
```

### COMMAND_TICKET (Request)
```json
{
  "service": { ... },
  "ticket": {
    "operation": "OPERATION_SELL",
    "dateTime": { "date": { "year": 2024, "month": 9, "day": 1 }, "time": { "hour": 12, "minute": 5, "second": 0 } },
    "operator": { "code": 1, "name": "Кассир 1" },
    "items": [
      {
        "type": "ITEM_TYPE_COMMODITY",
        "commodity": {
          "code": 123456,
          "name": "Товар 1",
          "sectionCode": "1",
          "quantity": 1,
          "price": { "bills": 1000, "coins": 0 },
          "sum": { "bills": 1000, "coins": 0 },
          "taxes": [ { "taxType": 1, "taxationType": 0, "percent": 1200, "sum": { "bills": 100, "coins": 0 }, "isInTotalSum": true } ],
          "listExciseStamp": [ "0000000000000" ],
          "physicalLabel": "ABC123",
          "productId": "1234567890",
          "barcode": "0200091530572",
          "ntin": "12345678901234",
          "measureUnitCode": "796"
        }
      }
    ],
    "payments": [
      {
        "type": "PAYMENT_CASH",
        "sum": { "bills": 1000, "coins": 0 },
        "cardPaymentFields": {
          "posTerminalId": "TERM-1",
          "posCardType": "VISA",
          "posAutorizationCode": 1234,
          "posRrn": 999999999999,
          "posReceiptNumber": 12
        },
        "mobilePaymentFields": {
          "qrType": "QR",
          "qrId": "QR123"
        }
      }
    ],
    "taxes": [
      { "taxType": 1, "taxationType": 0, "percent": 1200, "sum": { "bills": 100, "coins": 0 }, "isInTotalSum": true }
    ],
    "amounts": {
      "total": { "bills": 1000, "coins": 0 },
      "taken": { "bills": 1000, "coins": 0 },
      "change": { "bills": 0, "coins": 0 },
      "markup": { "name": "Наценка", "sum": { "bills": 10, "coins": 0 } },
      "discount": { "name": "Скидка", "sum": { "bills": 10, "coins": 0 } }
    },
    "extensionOptions": {
      "customerEmail": "user@example.com",
      "customerPhone": "+77000000000",
      "customerIinOrBin": "960624350642"
    },
    "offlineTicketNumber": 12,
    "printedTicket": "1",
    "frShiftNumber": 1,
    "shiftDocumentNumber": 1,
    "printedDocumentNumber": 123456,
    "parentTicket": {
      "parentTicketNumber": "123456789",
      "parentTicketDateTime": { "date": { "year": 2024, "month": 9, "day": 1 }, "time": { "hour": 9, "minute": 0, "second": 0 } },
      "kgdKkmId": "391827192812",
      "parentTicketTotal": { "bills": 1000, "coins": 0 },
      "parentTicketIsOffline": false
    }
  }
}
```

Key rules for `COMMAND_TICKET`: / Ключевые правила для `COMMAND_TICKET`:
- `items` is required and must contain at least one item. / `items` обязателен и содержит минимум один элемент.
- For `ITEM_TYPE_COMMODITY`, `name` or `code` is required. / Для `ITEM_TYPE_COMMODITY` обязателен `name` или `code`.
- `taxes` can be defined either at the ticket level or at the item level (mutually exclusive). / `taxes` либо на уровне чека, либо на уровне позиций (взаимоисключающие).
- For `PAYMENT_CASH`, `amounts.taken` and `amounts.change` are required. / Для `PAYMENT_CASH` обязательны `amounts.taken` и `amounts.change`.
- `parentTicket` is required for `OPERATION_BUY_RETURN` and `OPERATION_SELL_RETURN`. / `parentTicket` обязателен для `OPERATION_BUY_RETURN` и `OPERATION_SELL_RETURN`.
- `domain` is not used and is ignored by the library. / `domain` не используется и игнорируется библиотекой.

### COMMAND_TICKET (Response)
```json
{
  "commandType": "COMMAND_TICKET",
  "result": { ... },
  "service": { ... },
  "ticket": {
    "ticketNumber": "123456789",
    "qrCodeBase64": "aHR0cDovL2NvbnN1bWVyLnRlc3Qtb29mZC5rej9pPTEyMzQ="
  }
}
```

### COMMAND_REPORT (Request)
```json
{
  "service": { ... },
  "report": {
    "reportType": "REPORT_Z",
    "dateTime": { "date": { "year": 2024, "month": 9, "day": 1 }, "time": { "hour": 11, "minute": 0, "second": 0 } },
    "isOffline": false,
    "zxReport": { ... }
  }
}
```

### COMMAND_REPORT (Response)
```json
{
  "commandType": "COMMAND_REPORT",
  "result": { ... },
  "report": {
    "reportType": "REPORT_Z",
    "zxReport": { ... }
  },
  "service": { ... }
}
```

### COMMAND_CLOSE_SHIFT (Request)
```json
{
  "service": { ... },
  "closeShift": {
    "closeTime": { "date": { "year": 2024, "month": 9, "day": 1 }, "time": { "hour": 12, "minute": 0, "second": 0 } },
    "isOffline": false,
    "frShiftNumber": 12,
    "withdrawMoney": false,
    "printedDocumentNumber": 1001,
    "operator": { "code": 1, "name": "Operator 1" },
    "zReport": { ... }
  }
}
```

### COMMAND_CLOSE_SHIFT (Response)
```json
{
  "commandType": "COMMAND_CLOSE_SHIFT",
  "result": { ... },
  "report": {
    "reportType": "REPORT_Z",
    "zxReport": { ... }
  },
  "service": { ... }
}
```

## Common Blocks / Общие блоки

### Service (Request)
```json
{
  "getRegInfo": true,
  "offlinePeriod": {
    "beginTime": { "date": { "year": 2024, "month": 9, "day": 1 }, "time": { "hour": 10, "minute": 30, "second": 0 } },
    "endTime": { "date": { "year": 2024, "month": 9, "day": 1 }, "time": { "hour": 10, "minute": 40, "second": 0 } }
  },
  "securityStats": {
    "geoPosition": { "latitude": 432156, "longitude": 765432, "source": "CELL" }
  },
  "nomenclatureVersion": 1,
  "regInfo": {
    "kkm": {
      "pointOfPaymentNumber": "391827192812",
      "terminalNumber": "5465434234",
      "fnsKkmId": "201873",
      "serialNumber": "SN001",
      "kkmId": "K001"
    },
    "org": {
      "title": "Test Org",
      "address": "Test Address",
      "addressKz": "Test Address KZ",
      "inn": "960624350642",
      "okved": "123214214"
    }
  }
}
```

### Service (Response)
```json
{
  "regInfo": {
    "kkm": { "fnsKkmId": "...", "serialNumber": "...", "kkmId": "..." },
    "org": { "title": "...", "address": "...", "addressKz": "...", "inn": "...", "okved": "..." },
    "pos": { "title": "...", "address": "...", "addressKz": "...", "latitude": 0, "longitude": 0 }
  },
  "ticketAds": [
    { "info": { "type": "TICKET_AD_OFD", "version": 1 }, "text": "..." }
  ]
}
```

### ZXReport (максимальная структура)
```json
{
  "dateTime": { "date": { "year": 2024, "month": 9, "day": 1 }, "time": { "hour": 11, "minute": 0, "second": 0 } },
  "openShiftTime": { "date": { "year": 2024, "month": 9, "day": 1 }, "time": { "hour": 9, "minute": 0, "second": 0 } },
  "closeShiftTime": { "date": { "year": 2024, "month": 9, "day": 1 }, "time": { "hour": 12, "minute": 0, "second": 0 } },
  "shiftNumber": 1,
  "sections": [ { "sectionCode": "A", "operations": [ { "operation": "OPERATION_SELL", "count": 1, "sum": { "bills": 1000, "coins": 0 } } ] } ],
  "operations": [ { "operation": "OPERATION_SELL", "count": 1, "sum": { "bills": 1000, "coins": 0 } } ],
  "discounts": [ { "operation": "OPERATION_SELL", "count": 1, "sum": { "bills": 100, "coins": 0 } } ],
  "markups": [ { "operation": "OPERATION_SELL", "count": 1, "sum": { "bills": 50, "coins": 0 } } ],
  "totalResult": [ { "operation": "OPERATION_SELL", "count": 1, "sum": { "bills": 950, "coins": 0 } } ],
  "taxes": [
    {
      "taxType": 1,
      "percent": 1200,
      "operations": [
        { "operation": "OPERATION_SELL", "turnover": { "bills": 1000, "coins": 0 }, "sum": { "bills": 120, "coins": 0 }, "turnoverWithoutTax": { "bills": 880, "coins": 0 } }
      ]
    }
  ],
  "startShiftNonNullableSums": [ { "operation": "OPERATION_SELL", "sum": { "bills": 100, "coins": 0 } } ],
  "ticketOperations": [
    {
      "operation": "OPERATION_SELL",
      "ticketsTotalCount": 1,
      "ticketsCount": 1,
      "ticketsSum": { "bills": 1000, "coins": 0 },
      "payments": [ { "payment": "PAYMENT_CASH", "sum": { "bills": 1000, "coins": 0 }, "count": 1 } ],
      "offlineCount": 0,
      "discountSum": { "bills": 0, "coins": 0 },
      "markupSum": { "bills": 0, "coins": 0 },
      "changeSum": { "bills": 0, "coins": 0 }
    }
  ],
  "moneyPlacements": [
    {
      "operation": "MONEY_PLACEMENT_DEPOSIT",
      "operationsTotalCount": 1,
      "operationsCount": 1,
      "operationsSum": { "bills": 1000, "coins": 0 },
      "offlineCount": 0
    }
  ],
  "cashSum": { "bills": 10000, "coins": 0 },
  "revenue": { "sum": { "bills": 10000, "coins": 0 }, "isNegative": false },
  "nonNullableSums": [ { "operation": "OPERATION_SELL", "sum": { "bills": 100, "coins": 0 } } ]
}
```

---

## Kotlin Multiplatform & Swift Integration / Интеграция в Kotlin Multiplatform и Swift

`ofd-proto-codec` is a Kotlin Multiplatform (KMP) library compiling to JVM, Android, and Apple targets (`iosArm64`, `iosX64`, `iosSimulatorArm64`).

### Kotlin Multiplatform (KMP) usage
In `commonMain`, you can instantiate the codec and pass `kotlinx.serialization.json.JsonElement` objects:

```kotlin
import kotlinx.serialization.json.Json
import kz.mybrain.ofdcodec.application.OfdCodec
import kz.mybrain.ofdcodec.application.DefaultRegistry

val registry = DefaultRegistry.create()
val codec = OfdCodec(registry)

val jsonInput = Json.parseToJsonElement("""{"ofdId": "kazakhtelecom", ...}""")
val encodeResult = codec.encode(jsonInput)
if (encodeResult.isSuccess) {
    val jsonResponse = encodeResult.getOrNull()
    val base64Bytes = jsonResponse?.get("messageBase64")
}
```

### Swift (iOS/macOS) integration via SPM
In Swift, import the package `OfdProtoCodec` and use the class helper to serialize or deserialize. Because Kotlin's `Result` and `kotlinx.serialization` types are exported to Objective-C interfaces, we recommend calling the codec from your shared Kotlin module (`commonMain`) and presenting a simplified Swift-friendly API to Xcode.

If you instantiate it directly in Swift:
```swift
import OfdProtoCodec

let registry = DefaultRegistry.companion.create()
let codec = OfdCodec(registry: registry, ofdResolver: OfdCodecKt.defaultResolver())
```
