package kz.mybrain.ofdcodec

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kz.mybrain.ofdcodec.application.DefaultRegistry
import kz.mybrain.ofdcodec.application.OfdCodec
import kz.mybrain.ofdcodec.domain.model.OfdCodecException
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Редкие части чека и команда авторизации по протоколу 2.0.4.
 *
 * Сторно позиции, возврат по чеку-основанию и реквизиты покупателя приходят
 * не в каждом чеке, поэтому ломаются незаметно; авторизация кассира — вообще
 * отдельная команда, которую обычный чек не задействует.
 */
class BfdV204RareTicketPartsTest {

    private val codec = OfdCodec(DefaultRegistry.create())

    private fun money(bills: Long, coins: Int = 0) = """{ "bills": $bills, "coins": $coins }"""

    private fun errorsOf(exception: Throwable?): String =
        (exception as? OfdCodecException)?.errors?.joinToString("\n") {
            "${it.messageRu} | path=${it.path} | code=${it.code}"
        } ?: exception?.message.orEmpty()

    private fun envelope(commandType: String, block: String): JsonElement = Json.parseToJsonElement(
        """
        {
          "ofdId": "bfd",
          "protocolVersion": "204",
          "messageType": "REQUEST",
          "commandType": "$commandType",
          "header": { "deviceId": 201873, "token": 208627316, "reqNum": 4 },
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
$block
          }
        }
        """.trimIndent()
    )

    private fun assertAccepted(json: JsonElement, what: String) {
        val result = codec.encode(json)
        assertTrue(result.isSuccess, "$what обязан собираться: ${errorsOf(result.exceptionOrNull())}")
    }

    @Test
    fun shouldSerializeTicketWithStornoAndParentTicket() = assertAccepted(
        envelope(
            "COMMAND_TICKET",
            """
            "ticket": {
              "operation": "OPERATION_SELL_RETURN",
              "dateTime": {
                "date": { "year": 2024, "month": 9, "day": 1 },
                "time": { "hour": 12, "minute": 5, "second": 0 }
              },
              "operator": { "code": 1, "name": "Кассир 1" },
              "domain": { "type": "DOMAIN_TRADING" },
              "extensionOptions": {
                "customerEmail": "buyer@example.kz",
                "customerPhone": "+77010000000",
                "customerIinOrBin": "960624350642"
              },
              "parentTicket": {
                "parentTicketNumber": "1000",
                "parentTicketDateTime": {
                  "date": { "year": 2024, "month": 8, "day": 31 },
                  "time": { "hour": 18, "minute": 0, "second": 0 }
                },
                "kgdKkmId": "391827192812",
                "parentTicketTotal": ${money(1000)},
                "parentTicketIsOffline": false
              },
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
                },
                {
                  "type": "ITEM_TYPE_STORNO_COMMODITY",
                  "stornoCommodity": {
                    "name": "Товар 1",
                    "sectionCode": "1",
                    "quantity": 1,
                    "price": ${money(300)},
                    "sum": ${money(300)},
                    "measureUnitCode": "796",
                    "barcode": "4870004302037"
                  }
                }
              ],
              "payments": [
                {
                  "type": "PAYMENT_MOBILE",
                  "sum": ${money(700)},
                  "mobilePaymentFields": { "qrType": "KASPI", "qrId": "QR-77" }
                }
              ],
              "amounts": {
                "total": ${money(700)},
                "taken": ${money(700)},
                "change": ${money(0)}
              },
              "printedTicket": "2"
            }
            """.trimIndent()
        ),
        "Чек возврата со сторно и чеком-основанием"
    )

    @Test
    fun shouldSerializeAuthCommand() = assertAccepted(
        envelope(
            "COMMAND_AUTH",
            """
            "auth": { "login": "cashier1", "password": "s3cret" }
            """.trimIndent()
        ),
        "Команда авторизации"
    )

    @Test
    fun shouldRefuseAuthCommandWithoutPassword() {
        val result = codec.encode(
            envelope("COMMAND_AUTH", """"auth": { "login": "cashier1" }""")
        )

        assertTrue(result.isFailure, "Авторизация без пароля обязана быть отвергнута")
    }
}
