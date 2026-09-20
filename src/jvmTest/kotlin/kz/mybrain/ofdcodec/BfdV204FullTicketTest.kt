package kz.mybrain.ofdcodec

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kz.mybrain.ofdcodec.application.DefaultRegistry
import kz.mybrain.ofdcodec.application.OfdCodec
import kz.mybrain.ofdcodec.domain.model.OfdCodecException
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Чек 2.0.4 со всеми необязательными частями.
 *
 * Отраслевые реквизиты, сторно позиции, возврат по чеку-основанию, реквизиты
 * карточной и мобильной оплаты, номер автономного документа — всё то, что
 * в обычном чеке отсутствует и потому легко ломается незамеченным.
 */
class BfdV204FullTicketTest {

    private val codec = OfdCodec(DefaultRegistry.create())

    private fun money(bills: Long, coins: Int = 0) = """{ "bills": $bills, "coins": $coins }"""

    private fun errorsOf(exception: Throwable?): String =
        (exception as? OfdCodecException)?.errors?.joinToString("\n") {
            "${it.messageRu} | path=${it.path} | code=${it.code}"
        } ?: exception?.message.orEmpty()

    private fun ticket(domain: String, extras: String = ""): JsonElement = Json.parseToJsonElement(
        """
        {
          "ofdId": "bfd",
          "protocolVersion": "204",
          "messageType": "REQUEST",
          "commandType": "COMMAND_TICKET",
          "header": { "deviceId": 201873, "token": 208627316, "reqNum": 3 },
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
                    "measureUnitCode": "796",
                    "barcode": "4870004302037",
                    "productId": "PID-1",
                    "physicalLabel": "PL-1",
                    "ntin": "NTIN-1"
                  }
                }
              ],
              "payments": [
                {
                  "type": "PAYMENT_CARD",
                  "sum": ${money(1000)},
                  "cardPaymentFields": {
                    "posTerminalId": "TERM-1",
                    "posCardType": "VISA",
                    "posAutorizationCode": 123456
                  }
                }
              ],
              "amounts": {
                "total": ${money(1000)},
                "taken": ${money(1000)},
                "change": ${money(0)}
              },
              "printedTicket": "1"$extras
            }
          }
        }
        """.trimIndent()
    )

    private fun assertAccepted(json: JsonElement, what: String) {
        val result = codec.encode(json)
        assertTrue(result.isSuccess, "$what обязан собираться: ${errorsOf(result.exceptionOrNull())}")
    }

    @Test
    fun shouldSerializeTradingTicket() =
        assertAccepted(ticket("""{ "type": "DOMAIN_TRADING" }"""), "Чек торговли")

    @Test
    fun shouldSerializeServicesTicket() = assertAccepted(
        ticket("""{ "type": "DOMAIN_SERVICES", "services": { "accountNumber": "ACC-77" } }"""),
        "Чек услуг"
    )

    @Test
    fun shouldSerializeGasOilTicket() = assertAccepted(
        ticket(
            """
            {
              "type": "DOMAIN_GASOIL",
              "gasoil": {
                "cardNumber": "CARD-42",
                "correctionNumber": "COR-1",
                "correctionSum": ${money(50)}
              }
            }
            """.trimIndent()
        ),
        "Чек нефтепродуктов"
    )

    @Test
    fun shouldSerializeTaxiTicket() = assertAccepted(
        ticket(
            """
            {
              "type": "DOMAIN_TAXI",
              "taxi": { "carNumber": "123ABC", "isOrder": true, "currentFee": ${money(350)} }
            }
            """.trimIndent()
        ),
        "Чек такси"
    )

    @Test
    fun shouldSerializeParkingTicket() = assertAccepted(
        ticket(
            """
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
        ),
        "Чек стоянки"
    )

    @Test
    fun shouldSerializeOfflineTicketWithItsOwnNumber() = assertAccepted(
        ticket(
            """{ "type": "DOMAIN_TRADING" }""",
            extras = """,
              "offlineTicketNumber": 4294967295,
              "frShiftNumber": 12"""
        ),
        "Чек, оформленный в разрыве связи"
    )

    @Test
    fun shouldRefuseUnknownDomainType() {
        val result = codec.encode(ticket("""{ "type": "DOMAIN_КАКОЙ_ТО" }"""))

        assertTrue(result.isFailure, "Неизвестный вид отрасли обязан быть отвергнут")
        assertTrue(
            errorsOf(result.exceptionOrNull()).contains("DOMAIN_КАКОЙ_ТО"),
            "Отказ обязан называть значение: " + errorsOf(result.exceptionOrNull())
        )
    }
}
