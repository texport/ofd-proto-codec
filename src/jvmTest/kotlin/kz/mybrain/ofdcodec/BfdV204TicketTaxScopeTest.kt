package kz.mybrain.ofdcodec

import kotlin.test.Test
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json
import kz.mybrain.ofdcodec.application.DefaultRegistry
import kz.mybrain.ofdcodec.application.OfdCodec

/**
 * Налог стоит либо у позиций, либо у чека.
 *
 * Касса парка отвечает на чек с обоими уровнями отказом
 * `items_taxes_and_ticket_taxes_are_mutually_exclusive`. Отказ выдаётся
 * до отправки: после неё исправлять нечего — чек уже ушёл.
 */
class BfdV204TicketTaxScopeTest {

    @Test
    fun itemTaxesAloneAreAccepted() {
        val result = OfdCodec(DefaultRegistry.create()).encode(Json.parseToJsonElement(ticket(ticketTaxes = "")))

        assertTrue(
            result.isSuccess,
            "RU: Ожидается успешная сборка.\nEN: Expected success.\n" + result.exceptionOrNull()?.message
        )
    }

    @Test
    fun bothLevelsAtOnceAreRefused() {
        val result = OfdCodec(DefaultRegistry.create()).encode(
            Json.parseToJsonElement(
                ticket(
                    ticketTaxes = """"taxes": [ { "taxType": 100, "percent": 16000,
                        "sum": { "bills": 60, "coins": 12 }, "isInTotalSum": true } ],"""
                )
            )
        )

        assertTrue(result.isFailure, "RU: Ожидается отказ.\nEN: Expected failure.")
    }

    @Test
    fun ticketTaxesWithAStornoItemAreRefused() {
        // Сторнированная позиция несёт налоги в своём разделе: правило
        // про уровень, а не про вид позиции.
        val storno = """
                {
                  "type": "ITEM_TYPE_STORNO_COMMODITY",
                  "stornoCommodity": {
                    "name": "Кофе",
                    "sectionCode": "001",
                    "quantity": 1000,
                    "price": { "bills": 150, "coins": 55 },
                    "sum": { "bills": 150, "coins": 55 },
                    "measureUnitCode": "796",
                    "taxes": [
                      {
                        "taxType": 100,
                        "percent": 16000,
                        "sum": { "bills": 20, "coins": 76 },
                        "isInTotalSum": true
                      }
                    ]
                  }
                }
        """.trimIndent()
        val result = OfdCodec(DefaultRegistry.create()).encode(
            Json.parseToJsonElement(
                ticket(
                    ticketTaxes = """"taxes": [ { "taxType": 100, "percent": 16000,
                        "sum": { "bills": 20, "coins": 76 }, "isInTotalSum": true } ],""",
                    items = storno
                )
            )
        )

        assertTrue(result.isFailure, "RU: Ожидается отказ.\nEN: Expected failure.")
    }

    @Test
    fun ticketTaxesAloneAreAccepted() {
        // Налог на весь чек протоколом разрешён — запрещено только совмещать
        // его с налогами позиций.
        val result = OfdCodec(DefaultRegistry.create()).encode(
            Json.parseToJsonElement(
                ticket(
                    ticketTaxes = """"taxes": [ { "taxType": 100, "percent": 16000,
                        "sum": { "bills": 60, "coins": 12 }, "isInTotalSum": true } ],""",
                    items = PLAIN_ITEM
                )
            )
        )

        assertTrue(
            result.isSuccess,
            "RU: Ожидается успешная сборка.\nEN: Expected success.\n" + result.exceptionOrNull()?.message
        )
    }

    private fun ticket(ticketTaxes: String, items: String = COMMODITY_ITEM): String = """
        {
          "ofdId": "bfd",
          "protocolVersion": "204",
          "messageType": "REQUEST",
          "commandType": "COMMAND_TICKET",
          "header": { "deviceId": 201873, "token": 208627316, "reqNum": 1 },
          "payload": {
            "service": {
              "getRegInfo": false,
              "offlinePeriod": {
                "beginTime": {
                  "date": { "year": 2026, "month": 9, "day": 7 },
                  "time": { "hour": 10, "minute": 0, "second": 0 }
                },
                "endTime": {
                  "date": { "year": 2026, "month": 9, "day": 7 },
                  "time": { "hour": 10, "minute": 30, "second": 0 }
                }
              },
              "securityStats": {
                "geoPosition": { "latitude": 432156, "longitude": 765432, "source": "CELL" }
              },
              "regInfo": {
                "kkm": {
                  "fnsKkmId": "KGD-2000302",
                  "serialNumber": "KZT26E2C509A200",
                  "kkmId": "2000302"
                },
                "org": {
                  "title": "ТОО «Сарыарқа Сауда»",
                  "address": "Алматы, пр. Абая, 150",
                  "addressKz": "Алматы қаласы, Абай даңғылы, 150",
                  "inn": "123456789012",
                  "okved": "47111"
                }
              }
            },
            "ticket": {
              "operation": "OPERATION_SELL",
              "dateTime": {
                "date": { "year": 2026, "month": 9, "day": 7 },
                "time": { "hour": 10, "minute": 30, "second": 0 }
              },
              "operator": { "code": "1" },
              "domain": { "type": "DOMAIN_TRADING" },
              $ticketTaxes
              "items": [ $items ],
              "payments": [ { "type": "PAYMENT_CASH", "sum": { "bills": 435, "coins": 84 } } ],
              "amounts": {
                "total": { "bills": 435, "coins": 84 },
                "taken": { "bills": 435, "coins": 84 },
                "change": { "bills": 0, "coins": 0 }
              }
            }
          }
        }
    """.trimIndent()
}

/** Обычная позиция с собственным налогом. */
private val COMMODITY_ITEM = """
    {
      "type": "ITEM_TYPE_COMMODITY",
      "commodity": {
        "name": "Кофе",
        "sectionCode": "001",
        "quantity": 3000,
        "price": { "bills": 150, "coins": 55 },
        "sum": { "bills": 435, "coins": 84 },
        "measureUnitCode": "796",
        "taxes": [
          {
            "taxType": 100,
            "percent": 16000,
            "sum": { "bills": 60, "coins": 12 },
            "isInTotalSum": true
          }
        ]
      }
    }
""".trimIndent()

/** Позиция без собственных налогов. */
private val PLAIN_ITEM = """
    {
      "type": "ITEM_TYPE_COMMODITY",
      "commodity": {
        "name": "Кофе",
        "sectionCode": "001",
        "quantity": 3000,
        "price": { "bills": 150, "coins": 55 },
        "sum": { "bills": 435, "coins": 84 },
        "measureUnitCode": "796"
      }
    }
""".trimIndent()
