# OFD Proto Codec: JSON API

Этот документ описывает JSON‑форматы для `encode` (REQUEST) и `decode` (RESPONSE).
Все ошибки возвращаются сразу списком и содержат сообщения на русском и английском.

## Общий JSON‑конверт

### Запрос (encode)
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

### Ответ (decode)
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

### Result (общий блок ответа)
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

### Важные правила
- `protocolVersion` в формате `"203"`, без точек.
- `messageType` всегда `REQUEST` для encode и `RESPONSE` для decode.
- `header.size` и `messageBase64` возвращаются только из `encode`.
- `appCode` **не передается** в JSON и **не возвращается** в JSON.

## Токен и его смена

Токен меняется в ответе сервера. Новый токен берется из `header.token` ответа
и используется в следующем запросе.

Из известных команд, которые **меняют токен**:
- `COMMAND_TICKET`
- `COMMAND_CLOSE_SHIFT`

Если появятся другие команды с ротацией токена, их нужно добавить в этот список.

## Правила валидации

Библиотека валидирует **все** поля и возвращает **все** найденные ошибки одним списком.
Чтобы избежать ошибок:
- соблюдайте обязательные поля,
- не путайте типы (`string` вместо `number` и наоборот),
- следуйте диапазонам (`uint32`, `uint64`) из протокола.

## Поддерживаемые команды (v203)

- `COMMAND_AUTH`
- `COMMAND_SYSTEM`
- `COMMAND_INFO`
- `COMMAND_MONEY_PLACEMENT`
- `COMMAND_NOMENCLATURE`
- `COMMAND_REPORT`
- `COMMAND_TICKET`
- `COMMAND_CLOSE_SHIFT`

## Payload: структура по командам

Ниже приведены **максимально возможные** JSON‑структуры. Любые поля, отмеченные
как `optional`, можно не передавать.

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

`currentVersion` и `barcode` — хотя бы одно поле должно быть задано.

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

Ключевые правила для `COMMAND_TICKET`:
- `items` обязателен и содержит минимум один элемент.
- Для `ITEM_TYPE_COMMODITY` обязателен `name` или `code`.
- `taxes` либо на уровне чека, либо на уровне позиций (взаимоисключающие).
- Для `PAYMENT_CASH` обязательны `amounts.taken` и `amounts.change`.
- `parentTicket` обязателен для `OPERATION_BUY_RETURN` и `OPERATION_SELL_RETURN`.
- `domain` не используется и игнорируется библиотекой.

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

## Общие блоки

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
