package kz.mybrain.ofdcodec

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kz.mybrain.ofdcodec.application.DefaultRegistry
import kz.mybrain.ofdcodec.application.OfdCodec
import kz.mybrain.ofdcodec.domain.model.OfdCodecException
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Отраслевые реквизиты и виды оплаты на обеих версиях протокола.
 *
 * В 2.0.3 виды оплаты «в кредит» и «тарой» существуют, в 2.0.4 они убраны.
 * Разница между версиями обязана выражаться именованным отказом, а не сбоем
 * сериализации с именем сгенерированного класса наружу.
 */
class DomainAndPaymentAcrossVersionsTest {

    private val codec = OfdCodec(DefaultRegistry.create())

    private fun money(bills: Long, coins: Int = 0) = """{ "bills": $bills, "coins": $coins }"""

    private fun errorsOf(exception: Throwable?): String =
        (exception as? OfdCodecException)?.errors?.joinToString("\n") {
            "${it.messageRu} | path=${it.path} | code=${it.code}"
        } ?: exception?.message.orEmpty()

    private fun ticket(ofdId: String, version: String, domain: String, payment: String): JsonElement =
        Json.parseToJsonElement(
            """
            {
              "ofdId": "$ofdId",
              "protocolVersion": "$version",
              "messageType": "REQUEST",
              "commandType": "COMMAND_TICKET",
              "header": { "deviceId": 201873, "token": 208627316, "reqNum": 6 },
          "payload": {
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
                  "title": "ИП МИЧКА ПАВЕЛ АНДРЕЕВИЧ",
                  "address": "обл. Павлодарская, Ауэзова 88",
                  "addressKz": "Республика Қазақстан, Ауэзова 88",
                  "inn": "960624350642",
                  "okved": "47301"
                }
              }
            },
            "ticket": {
                  "operation": "OPERATION_SELL",
                  "dateTime": {
                    "date": { "year": 2024, "month": 9, "day": 1 },
                    "time": { "hour": 12, "minute": 5, "second": 0 }
                  },
                  "operator": { "code": 1, "name": "Кассир 1" },
                  "domain": $domain,
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
                  "payments": [ { "type": "$payment", "sum": ${money(1000)} } ],
                  "amounts": {
                    "total": ${money(1000)},
                    "taken": ${money(1000)},
                    "change": ${money(0)}
                  },
                  "printedTicket": "1"
                }
              }
            }
            """.trimIndent()
        )

    private val taxi = """
        { "type": "DOMAIN_TAXI", "taxi": { "carNumber": "123ABC", "isOrder": true, "currentFee": ${money(350)} } }
    """.trimIndent()

    private val parking = """
        {
          "type": "DOMAIN_PARKING",
          "parking": {
            "beginTime": {
              "date": { "year": 2024, "month": 9, "day": 1 },
              "time": { "hour": 10, "minute": 0, "second": 0 }
            },
            "endTime": {
              "date": { "year": 2024, "month": 9, "day": 1 },
              "time": { "hour": 12, "minute": 0, "second": 0 }
            }
          }
        }
    """.trimIndent()

    private val gasoil = """
        {
          "type": "DOMAIN_GASOIL",
          "gasoil": { "cardNumber": "CARD-42", "correctionNumber": "COR-1", "correctionSum": ${money(50)} }
        }
    """.trimIndent()

    private val services = """{ "type": "DOMAIN_SERVICES", "services": { "accountNumber": "ACC-77" } }"""

    private fun accepted(json: JsonElement, what: String) {
        val result = codec.encode(json)
        assertTrue(result.isSuccess, "$what обязан собираться: ${errorsOf(result.exceptionOrNull())}")
    }

    @Test
    fun shouldSerializeEveryDomainOn203() {
        for (domain in listOf(services, gasoil, taxi, parking)) {
            accepted(ticket("kazakhtelecom", "203", domain, "PAYMENT_CASH"), "Отраслевой чек 2.0.3")
        }
    }

    @Test
    fun shouldAcceptCreditOn203() =
        accepted(
            ticket("kazakhtelecom", "203", """{ "type": "DOMAIN_TRADING" }""", "PAYMENT_CREDIT"),
            "Оплата в кредит по 2.0.3"
        )

    @Test
    fun shouldAcceptTareOn203() =
        accepted(
            ticket("kazakhtelecom", "203", """{ "type": "DOMAIN_TRADING" }""", "PAYMENT_TARE"),
            "Оплата тарой по 2.0.3"
        )

    @Test
    fun shouldRefuseCreditOn204ByName() {
        val result = codec.encode(
            ticket("bfd", "204", """{ "type": "DOMAIN_TRADING" }""", "PAYMENT_CREDIT")
        )

        assertTrue(result.isFailure, "В 2.0.4 вида оплаты «в кредит» нет")
        val reason = errorsOf(result.exceptionOrNull())
        assertTrue(reason.contains("PAYMENT_CREDIT"), "Отказ обязан называть вид оплаты: $reason")
        assertTrue(reason.contains("2.0.4"), "Отказ обязан называть версию протокола: $reason")
    }

    @Test
    fun shouldRefuseTareOn204ByName() {
        val result = codec.encode(
            ticket("bfd", "204", """{ "type": "DOMAIN_TRADING" }""", "PAYMENT_TARE")
        )

        assertTrue(result.isFailure, "В 2.0.4 вида оплаты тарой нет")
        assertTrue(
            errorsOf(result.exceptionOrNull()).contains("PAYMENT_TARE"),
            "Отказ обязан называть вид оплаты"
        )
    }
}
