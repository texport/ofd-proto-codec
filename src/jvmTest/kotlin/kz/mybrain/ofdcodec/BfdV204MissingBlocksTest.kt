package kz.mybrain.ofdcodec

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kz.mybrain.ofdcodec.application.DefaultRegistry
import kz.mybrain.ofdcodec.application.OfdCodec
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Отказы кодека 2.0.4 при отсутствии обязательных блоков.
 *
 * Проверяется не только сам отказ, но и то, что он приходит на каждый
 * недостающий блок по отдельности: иначе одна забытая проверка прячется
 * за другой, и запрос без служебных данных уходит в ОФД.
 */
class BfdV204MissingBlocksTest {

    private val codec = OfdCodec(DefaultRegistry.create())

    private fun money(bills: Long) = """{ "bills": $bills, "coins": 0 }"""

    private val offlinePeriod = """
        "offlinePeriod": {
          "beginTime": {
            "date": { "year": 2024, "month": 9, "day": 1 },
            "time": { "hour": 10, "minute": 30, "second": 0 }
          },
          "endTime": {
            "date": { "year": 2024, "month": 9, "day": 1 },
            "time": { "hour": 10, "minute": 40, "second": 0 }
          }
        }
    """.trimIndent()

    private val regInfo = """
        "regInfo": {
          "kkm": {
            "fnsKkmId": "391827192812",
            "serialNumber": "5465434234",
            "kkmId": "201873"
          },
          "org": {
            "title": "ИП",
            "address": "Алматы",
            "addressKz": "Алматы",
            "inn": "960624350642",
            "okved": "47301"
          }
        }
    """.trimIndent()

    private val securityStats = """
        "securityStats": {
          "geoPosition": { "latitude": 432156, "longitude": 765432, "source": "CELL" }
        }
    """.trimIndent()

    private fun service(
        withOfflinePeriod: Boolean = true,
        withRegInfo: Boolean = true,
        withSecurityStats: Boolean = true
    ): String {
        val parts = buildList {
            add(""""getRegInfo": true""")
            if (withOfflinePeriod) add(offlinePeriod)
            if (withSecurityStats) add(securityStats)
            if (withRegInfo) add(regInfo)
        }
        return """"service": { ${parts.joinToString(", ")} }"""
    }

    private val fullService get() = service()

    private val ticket = """
        "ticket": {
          "operation": "OPERATION_SELL",
          "dateTime": {
            "date": { "year": 2024, "month": 9, "day": 1 },
            "time": { "hour": 12, "minute": 5, "second": 0 }
          },
          "operator": { "code": 1, "name": "Кассир 1" },
          "domain": { "type": "DOMAIN_TRADING" },
          "items": [
            {
              "type": "ITEM_TYPE_COMMODITY",
              "commodity": {
                "name": "Товар 1",
                "sectionCode": "1",
                "quantity": 1,
                "price": ${money(1000)},
                "sum": ${money(1000)},
                "measureUnitCode": "796"
              }
            }
          ],
          "payments": [ { "type": "PAYMENT_CASH", "sum": ${money(1000)} } ],
          "amounts": {
            "total": ${money(1000)},
            "taken": ${money(1000)},
            "change": ${money(0)}
          },
          "printedTicket": "1"
        }
    """.trimIndent()

    private fun request(commandType: String, payload: String): JsonElement = Json.parseToJsonElement(
        """
        {
          "ofdId": "bfd",
          "protocolVersion": "204",
          "messageType": "REQUEST",
          "commandType": "$commandType",
          "header": { "deviceId": 201873, "token": 208627316, "reqNum": 2 },
          "payload": { $payload }
        }
        """.trimIndent()
    )

    private fun assertRefused(payload: String, what: String, commandType: String = "COMMAND_TICKET") {
        val result = codec.encode(request(commandType, payload))
        assertTrue(result.isFailure, "$what обязан быть отвергнут")
    }

    @Test
    fun shouldRefuseTicketWithoutServiceBlock() =
        assertRefused(ticket, "Запрос без служебного блока")

    @Test
    fun shouldRefuseTicketWithoutTicketBlock() =
        assertRefused(fullService, "Запрос чека без самого чека")

    @Test
    fun shouldRefuseServiceWithoutOfflinePeriod() = assertRefused(
        "${service(withOfflinePeriod = false)}, $ticket",
        "Служебный блок без периода автономной работы"
    )

    @Test
    fun shouldRefuseServiceWithoutRegistrationInfo() = assertRefused(
        "${service(withRegInfo = false)}, $ticket",
        "Служебный блок без регистрационных данных"
    )

    @Test
    fun shouldRefuseServiceWithoutSecurityStats() = assertRefused(
        "${service(withSecurityStats = false)}, $ticket",
        "Служебный блок без данных о безопасности"
    )

    @Test
    fun shouldRefuseUnknownOfdProvider() {
        val json = Json.parseToJsonElement(
            """
            {
              "ofdId": "нет-такого-офд",
              "protocolVersion": "204",
              "messageType": "REQUEST",
              "commandType": "COMMAND_TICKET",
              "header": { "deviceId": 201873, "token": 208627316, "reqNum": 2 },
              "payload": { $fullService, $ticket }
            }
            """.trimIndent()
        )

        assertTrue(codec.encode(json).isFailure, "Неизвестный ОФД обязан быть отвергнут")
    }

    @Test
    fun shouldRefuseUnknownProtocolVersion() {
        val json = Json.parseToJsonElement(
            """
            {
              "ofdId": "bfd",
              "protocolVersion": "999",
              "messageType": "REQUEST",
              "commandType": "COMMAND_TICKET",
              "header": { "deviceId": 201873, "token": 208627316, "reqNum": 2 },
              "payload": { $fullService, $ticket }
            }
            """.trimIndent()
        )

        assertTrue(codec.encode(json).isFailure, "Неизвестная версия протокола обязана быть отвергнута")
    }

    private fun ticketWith(body: String): String = """
        "ticket": {
          "operation": "OPERATION_SELL",
          "dateTime": {
            "date": { "year": 2024, "month": 9, "day": 1 },
            "time": { "hour": 12, "minute": 5, "second": 0 }
          },
          "operator": { "code": 1, "name": "Кассир 1" },
          "domain": { "type": "DOMAIN_TRADING" },
          $body
        }
    """.trimIndent()

    private val oneItem = """
        "items": [
          {
            "type": "ITEM_TYPE_COMMODITY",
            "commodity": {
              "name": "Товар 1",
              "sectionCode": "1",
              "quantity": 1,
              "price": ${money(1000)},
              "sum": ${money(1000)},
              "measureUnitCode": "796"
            }
          }
        ]
    """.trimIndent()

    private val cashAmounts = """
        "amounts": {
          "total": ${money(1000)},
          "taken": ${money(1000)},
          "change": ${money(0)}
        },
        "printedTicket": "1"
    """.trimIndent()

    @Test
    fun shouldRefuseTicketWithEmptyItemList() = assertRefused(
        "$fullService, ${ticketWith(""""items": [], "payments": [ { "type": "PAYMENT_CASH", "sum": ${money(1000)} } ], $cashAmounts""")}",
        "Чек с пустым списком позиций"
    )

    @Test
    fun shouldRefuseTicketWithRepeatedPaymentType() = assertRefused(
        "$fullService, ${ticketWith("""$oneItem, "payments": [ { "type": "PAYMENT_CASH", "sum": ${money(500)} }, { "type": "PAYMENT_CASH", "sum": ${money(500)} } ], $cashAmounts""")}",
        "Чек с двумя одинаковыми видами оплаты"
    )

    @Test
    fun shouldRefuseTicketWithRepeatedTaxPercent() {
        val tax = """{ "taxType": 100, "percent": 16000, "sum": ${money(138)}, "isInTotalSum": true }"""
        assertRefused(
            "$fullService, ${ticketWith("""$oneItem, "taxes": [ $tax, $tax ], "payments": [ { "type": "PAYMENT_CARD", "sum": ${money(1000)} } ], "amounts": { "total": ${money(1000)} }, "printedTicket": "1"""")}",
            "Чек с двумя налогами одной ставки"
        )
    }

    @Test
    fun shouldRefuseTicketWhereTaxIsNotAnObject() = assertRefused(
        "$fullService, ${ticketWith("""$oneItem, "taxes": [ 42 ], "payments": [ { "type": "PAYMENT_CARD", "sum": ${money(1000)} } ], "amounts": { "total": ${money(1000)} }, "printedTicket": "1"""")}",
        "Чек, где налог не объект"
    )

    @Test
    fun shouldRefuseReturnWithoutParentTicket() {
        val body = """$oneItem, "payments": [ { "type": "PAYMENT_CARD", "sum": ${money(1000)} } ], "amounts": { "total": ${money(1000)} }, "printedTicket": "1""""
        val ticketJson = ticketWith(body).replace("OPERATION_SELL", "OPERATION_SELL_RETURN")
        assertRefused("$fullService, $ticketJson", "Возврат без чека-основания")
    }

    @Test
    fun shouldAcceptCardOnlyTicketWithoutTakenAndChange() {
        // Принятое и сдача требуются только при наличной оплате: карточный
        // чек обязан проходить без них.
        val payload = "$fullService, ${ticketWith("""$oneItem, "payments": [ { "type": "PAYMENT_CARD", "sum": ${money(1000)} } ], "amounts": { "total": ${money(1000)} }, "printedTicket": "1"""")}"

        assertTrue(
            codec.encode(request("COMMAND_TICKET", payload)).isSuccess,
            "Чек только с картой обязан собираться без принятого и сдачи"
        )
    }

    @Test
    fun shouldRefuseOfflineTicketNumberOfAWrongKind() {
        // Проверка запроса обязана различать «не число» и «число вне
        // диапазона»: и то и другое отвергается, но по разным причинам.
        val tail = """"payments": [ { "type": "PAYMENT_CARD", "sum": ${money(1000)} } ], "amounts": { "total": ${money(1000)} }, "printedTicket": "1""""

        assertRefused(
            "$fullService, ${ticketWith("""$oneItem, $tail, "offlineTicketNumber": "не число"""")}",
            "Номер автономного документа строкой"
        )
        assertRefused(
            "$fullService, ${ticketWith("""$oneItem, $tail, "offlineTicketNumber": 4294967296""")}",
            "Номер автономного документа за пределом диапазона"
        )
        assertRefused(
            "$fullService, ${ticketWith("""$oneItem, $tail, "frShiftNumber": -1""")}",
            "Отрицательный номер смены"
        )
    }

    @Test
    fun shouldAcceptOfflineTicketNumberAtTheTopOfTheRange() {
        // Верхняя граница диапазона обязана проходить: спецификация прямо
        // предлагает брать под номер младшие четыре байта unixtime.
        val tail = """"payments": [ { "type": "PAYMENT_CARD", "sum": ${money(1000)} } ], "amounts": { "total": ${money(1000)} }, "printedTicket": "1""""
        val payload = "$fullService, ${ticketWith("""$oneItem, $tail, "offlineTicketNumber": 4294967295""")}"

        assertTrue(
            codec.encode(request("COMMAND_TICKET", payload)).isSuccess,
            "Наибольший допустимый номер автономного документа обязан проходить"
        )
    }

    @Test
    fun shouldRefuseTicketPartsOfAWrongShape() {
        // Разбор запроса не вправе считать, что клиент прислал именно то,
        // что обещал: список оплат строкой, оплата не объектом, вид оплаты
        // числом и чек-основание не объектом обязаны быть отвергнуты.
        val amounts = """"amounts": { "total": ${money(1000)} }, "printedTicket": "1""""

        assertRefused(
            "$fullService, ${ticketWith("""$oneItem, "payments": "не список", $amounts""")}",
            "Список оплат строкой"
        )
        assertRefused(
            "$fullService, ${ticketWith("""$oneItem, "payments": [ 42 ], $amounts""")}",
            "Оплата не объектом"
        )
        assertRefused(
            "$fullService, ${ticketWith("""$oneItem, "payments": [ { "type": 1, "sum": ${money(1000)} } ], $amounts""")}",
            "Вид оплаты числом"
        )
        assertRefused(
            "$fullService, ${ticketWith("""$oneItem, "payments": [ { "type": "PAYMENT_CARD", "sum": ${money(1000)} } ], "parentTicket": "не объект", $amounts""")}",
            "Чек-основание не объектом"
        )
        assertRefused(
            "$fullService, ${ticketWith(""""items": "не список", "payments": [ { "type": "PAYMENT_CARD", "sum": ${money(1000)} } ], $amounts""")}",
            "Список позиций строкой"
        )
    }
}
