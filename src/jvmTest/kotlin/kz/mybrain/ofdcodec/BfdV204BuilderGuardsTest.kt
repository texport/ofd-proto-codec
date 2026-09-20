package kz.mybrain.ofdcodec

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kz.mybrain.ofdcodec.ofd.bfd.v204.codec.closeshift.CloseShiftRequestBuilder
import kz.mybrain.ofdcodec.ofd.bfd.v204.codec.command.CommandAuthRequestBuilder
import kz.mybrain.ofdcodec.ofd.bfd.v204.codec.common.DateTimeBuilder
import kz.mybrain.ofdcodec.ofd.bfd.v204.codec.moneyplacement.MoneyPlacementRequestBuilder
import kz.mybrain.ofdcodec.ofd.bfd.v204.codec.nomenclature.NomenclatureRequestBuilder
import kz.mybrain.ofdcodec.ofd.bfd.v204.codec.report.ReportRequestBuilder
import kz.mybrain.ofdcodec.ofd.bfd.v204.codec.report.ZXReportBuilder
import kz.mybrain.ofdcodec.ofd.bfd.v204.codec.service.ServiceRequestBuilder
import kz.mybrain.ofdcodec.ofd.bfd.v204.codec.ticket.TicketRequestBuilder
import kotlin.test.Test
import kz.mybrain.ofdcodec.ofd.bfd.v204.codec.service.OrgRegInfoBuilder
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * Защитные отказы сборщиков запроса 2.0.4.
 *
 * До сборщика запрос обычно не доходит: проверка отвергает его раньше.
 * Но защита в сборщике — последний рубеж, и её отказ обязан называть
 * недостающий блок, а не падать безымянно. Через codec.encode эти ветви
 * недостижимы, поэтому сборщики вызываются напрямую.
 */
class BfdV204BuilderGuardsTest {

    private fun payload(body: String): JsonObject =
        Json.parseToJsonElement(body) as JsonObject

    private val empty = payload("{}")

    private val serviceBlock = """
        "service": {
          "getRegInfo": true,
          "offlinePeriod": {
            "beginTime": {
              "date": { "year": 2024, "month": 9, "day": 1 },
              "time": { "hour": 10, "minute": 30, "second": 0 }
            },
            "endTime": {
              "date": { "year": 2024, "month": 9, "day": 1 },
              "time": { "hour": 10, "minute": 40, "second": 0 }
            }
          },
          "securityStats": {
            "geoPosition": { "latitude": 432156, "longitude": 765432, "source": "CELL" }
          },
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
        }
    """.trimIndent()


    private fun assertNamedRefusal(what: String, mustMention: String, block: () -> Unit) {
        try {
            block()
            fail("$what обязан быть отвергнут")
        } catch (failure: IllegalArgumentException) {
            assertTrue(
                failure.message?.contains(mustMention) == true,
                "Отказ обязан называть «$mustMention», получено: ${failure.message}"
            )
        }
    }

    @Test
    fun shouldNameTheMissingTicketBlock() =
        assertNamedRefusal("Чек без блока ticket", "ticket") { TicketRequestBuilder().build(empty) }

    @Test
    fun shouldNameTheMissingReportBlock() =
        assertNamedRefusal("Отчёт без блока report", "report") { ReportRequestBuilder().build(empty) }

    @Test
    fun shouldNameTheMissingZxReportBlock() = assertNamedRefusal(
        "Отчёт без zxReport",
        "zxReport"
    ) { ReportRequestBuilder().build(payload("""{ "report": { "reportType": "REPORT_Z" } }""")) }

    @Test
    fun shouldNameTheMissingNomenclatureBlock() = assertNamedRefusal(
        "Справочник без блока nomenclature",
        "nomenclature"
    ) { NomenclatureRequestBuilder().build(empty) }

    @Test
    fun shouldNameTheMissingCloseShiftBlock() = assertNamedRefusal(
        "Закрытие смены без блока closeShift",
        "closeShift"
    ) { CloseShiftRequestBuilder().build(empty) }

    @Test
    fun shouldNameTheMissingMoneyPlacementBlock() = assertNamedRefusal(
        "Движение денег без блока moneyPlacement",
        "moneyPlacement"
    ) { MoneyPlacementRequestBuilder().build(empty) }

    @Test
    fun shouldNameTheMissingMoneyPlacementOperator() = assertNamedRefusal(
        "Движение денег без кассира",
        "operator"
    ) {
        MoneyPlacementRequestBuilder().build(
            payload("""{ "moneyPlacement": { "operation": "MONEY_PLACEMENT_DEPOSIT" } }""")
        )
    }

    @Test
    fun shouldNameTheMissingAuthBlock() = assertNamedRefusal(
        "Авторизация без блока auth",
        "auth"
    ) { CommandAuthRequestBuilder().build(payload("{ $serviceBlock }")) }

    @Test
    fun shouldNameTheMissingPassword() = assertNamedRefusal(
        "Авторизация без пароля",
        "password"
    ) {
        CommandAuthRequestBuilder().build(
            payload("""{ $serviceBlock, "auth": { "login": "cashier1" } }""")
        )
    }

    @Test
    fun shouldNameTheMissingOfflinePeriod() = assertNamedRefusal(
        "Служебный блок без периода автономной работы",
        "offlinePeriod"
    ) { ServiceRequestBuilder().build(payload("""{ "service": { "getRegInfo": true } }""")) }

    @Test
    fun shouldNameTheMissingDateTimeParts() {
        val builder = DateTimeBuilder()
        assertNamedRefusal("Момент времени без самого поля", "dateTime") { builder.build(empty, "dateTime") }
        assertNamedRefusal("Момент времени без даты", "dateTime.date") {
            builder.build(payload("""{ "dateTime": { "time": { "hour": 1, "minute": 2, "second": 3 } } }"""), "dateTime")
        }
        assertNamedRefusal("Момент времени без времени", "dateTime.time") {
            builder.build(payload("""{ "dateTime": { "date": { "year": 2024, "month": 9, "day": 1 } } }"""), "dateTime")
        }
    }

    @Test
    fun shouldNameTheMissingReportParts() {
        val builder = ZXReportBuilder()
        val moment = """
            "dateTime": {
              "date": { "year": 2024, "month": 9, "day": 1 },
              "time": { "hour": 23, "minute": 0, "second": 0 }
            },
            "openShiftTime": {
              "date": { "year": 2024, "month": 9, "day": 1 },
              "time": { "hour": 8, "minute": 0, "second": 0 }
            },
            "shiftNumber": 1,
            "cashSum": { "bills": 0, "coins": 0 },
            "revenue": { "sum": { "bills": 0, "coins": 0 }, "isNegative": false }
        """.trimIndent()

        assertNamedRefusal("Секция без операций", "operations") {
            builder.build(payload("""{ $moment, "sections": [ { "sectionCode": "1" } ] }"""))
        }
        assertNamedRefusal("Налог без операций", "operations") {
            builder.build(payload("""{ $moment, "taxes": [ { "taxType": 100, "percent": 16000 } ] }"""))
        }
        assertNamedRefusal("Операция по чекам без видов оплаты", "payments") {
            builder.build(
                payload(
                    """
                    {
                      $moment,
                      "ticketOperations": [
                        {
                          "operation": "OPERATION_SELL",
                          "ticketsTotalCount": 1,
                          "ticketsCount": 1,
                          "ticketsSum": { "bills": 100, "coins": 0 },
                          "offlineCount": 0,
                          "discountSum": { "bills": 0, "coins": 0 },
                          "markupSum": { "bills": 0, "coins": 0 },
                          "changeSum": { "bills": 0, "coins": 0 }
                        }
                      ]
                    }
                    """.trimIndent()
                )
            )
        }
    }

    @Test
    fun shouldBuildOrganisationWithEitherSpellingOfTheActivityCode() {
        // В 2.0.3 поле называлось okved, в 2.0.4 — oked. Принимаются оба,
        // иначе касса, обновлённая до 2.0.4, потеряла бы код деятельности.
        val builder = OrgRegInfoBuilder()
        val common = """"title": "ИП", "address": "Алматы", "addressKz": "Алматы", "inn": "960624350642""""

        assertEquals("47301", builder.build(payload("{ $common, \"oked\": \"47301\" }")).oked)
        assertEquals("47301", builder.build(payload("{ $common, \"okved\": \"47301\" }")).oked)
        assertNamedRefusal("Организация без кода деятельности", "oked") {
            builder.build(payload("{ $common }"))
        }
    }

    @Test
    fun shouldBuildMinimalTicketWithoutOptionalBlocks() {
        // Чек без видов оплаты, налогов, сумм принятого и сдачи: так
        // проверяются ветви «поля нет», недостижимые через полный запрос.
        val ticket = TicketRequestBuilder().build(
            payload(
                """
                {
                  "ticket": {
                    "operation": "OPERATION_SELL",
                    "dateTime": {
                      "date": { "year": 2024, "month": 9, "day": 1 },
                      "time": { "hour": 12, "minute": 5, "second": 0 }
                    },
                    "operator": { "code": 1, "name": "Кассир 1" },
                    "items": [
                      {
                        "type": "ITEM_TYPE_COMMODITY",
                        "commodity": {
                          "code": 12345,
                          "sectionCode": "1",
                          "quantity": 1,
                          "price": { "bills": 1000, "coins": 0 },
                          "sum": { "bills": 1000, "coins": 0 },
                          "measureUnitCode": "796"
                        }
                      }
                    ],
                    "amounts": { "total": { "bills": 1000, "coins": 0 } }
                  }
                }
                """.trimIndent()
            )
        )

        assertTrue(ticket.payments.isEmpty(), "Видов оплаты не было — их и не должно появиться")
        assertTrue(ticket.taxes.isEmpty(), "Налогов не было — их и не должно появиться")
    }

    @Test
    fun shouldRefuseServiceRequestWithoutServiceBlock() {
        // В 2.0.4 регистрационные данные обязательны, поэтому запрос без
        // служебного блока не собирается вовсе — и отказ это называет.
        assertNamedRefusal("Запрос без служебного блока", "regInfo") {
            ServiceRequestBuilder().build(empty)
        }
    }

    @Test
    fun shouldNameEachMissingPartOfTheServiceBlock() {
        val builder = ServiceRequestBuilder()
        val offline = """
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
        val security = """
            "securityStats": {
              "geoPosition": { "latitude": 432156, "longitude": 765432, "source": "CELL" }
            }
        """.trimIndent()
        val org = """
            "org": {
              "title": "ИП",
              "address": "Алматы",
              "addressKz": "Алматы",
              "inn": "960624350642",
              "okved": "47301"
            }
        """.trimIndent()
        val kkm = """
            "kkm": {
              "fnsKkmId": "391827192812",
              "serialNumber": "5465434234",
              "kkmId": "201873"
            }
        """.trimIndent()

        assertNamedRefusal("Служебный блок без данных о безопасности", "securityStats") {
            builder.build(payload("""{ "service": { "getRegInfo": true, $offline } }"""))
        }
        assertNamedRefusal("Регистрационные данные без кассы", "regInfo.kkm") {
            builder.build(
                payload("""{ "service": { "getRegInfo": true, $offline, $security, "regInfo": { $org } } }""")
            )
        }
        assertNamedRefusal("Регистрационные данные без организации", "regInfo.org") {
            builder.build(
                payload("""{ "service": { "getRegInfo": true, $offline, $security, "regInfo": { $kkm } } }""")
            )
        }
    }

    @Test
    fun shouldRefuseOfflineTicketNumberOutsideTheUnsignedRange() {
        // Диапазон поля — 0..4294967295 из спецификации. И перебор, и текст
        // вместо числа обязаны отвергаться.
        val builder = TicketRequestBuilder()
        val head = """
            "operation": "OPERATION_SELL",
            "dateTime": {
              "date": { "year": 2024, "month": 9, "day": 1 },
              "time": { "hour": 12, "minute": 5, "second": 0 }
            },
            "operator": { "code": 1, "name": "Кассир 1" },
            "items": [
              {
                "type": "ITEM_TYPE_COMMODITY",
                "commodity": {
                  "name": "Товар 1",
                  "sectionCode": "1",
                  "quantity": 1,
                  "price": { "bills": 1000, "coins": 0 },
                  "sum": { "bills": 1000, "coins": 0 },
                  "measureUnitCode": "796"
                }
              }
            ],
            "amounts": { "total": { "bills": 1000, "coins": 0 } }
        """.trimIndent()

        assertNamedRefusal("Номер автономного документа за пределом диапазона", "uint32") {
            builder.build(payload("""{ "ticket": { $head, "offlineTicketNumber": 4294967296 } }"""))
        }
    }
}
