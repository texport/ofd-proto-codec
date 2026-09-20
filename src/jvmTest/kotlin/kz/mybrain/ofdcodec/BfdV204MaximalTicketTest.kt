package kz.mybrain.ofdcodec

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kz.mybrain.ofdcodec.application.DefaultRegistry
import kz.mybrain.ofdcodec.application.OfdCodec
import kz.mybrain.ofdcodec.domain.model.OfdCodecException
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Предельный чек 2.0.4 и отказы на неполных данных.
 *
 * Здесь собраны те виды позиций, которых не бывает в обычной продаже:
 * наценка и скидка отдельными позициями, их сторно, налоги на позиции
 * и на чек, акцизные марки. Рядом — отказы: без них не видно, что кодек
 * различает «поля нет» и «поле неверное».
 */
class BfdV204MaximalTicketTest {

    private val codec = OfdCodec(DefaultRegistry.create())

    private fun money(bills: Long, coins: Int = 0) = """{ "bills": $bills, "coins": $coins }"""

    private val tax = """
        {
          "taxType": 100,
          "taxationType": 100,
          "percent": 16000,
          "sum": ${money(138)},
          "isInTotalSum": true
        }
    """.trimIndent()

    private fun errorsOf(exception: Throwable?): String =
        (exception as? OfdCodecException)?.errors?.joinToString("\n") {
            "${it.messageRu} | path=${it.path} | code=${it.code}"
        } ?: exception?.message.orEmpty()

    private fun envelope(ticketBlock: String): JsonElement = Json.parseToJsonElement(
        """
        {
          "ofdId": "bfd",
          "protocolVersion": "204",
          "messageType": "REQUEST",
          "commandType": "COMMAND_TICKET",
          "header": { "deviceId": 201873, "token": 208627316, "reqNum": 8 },
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
            "ticket": $ticketBlock
          }
        }
        """.trimIndent()
    )

    private val maximalTicket = """
        {
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
                "code": 12345,
                "sectionCode": "1",
                "quantity": 1,
                "price": ${money(1000)},
                "sum": ${money(1000)},
                "measureUnitCode": "796",
                "taxes": [ $tax ],
                "listExciseStamp": [ "СТАМП-1", "СТАМП-2" ]
              }
            },
            {
              "type": "ITEM_TYPE_MARKUP",
              "markup": { "name": "Наценка", "sum": ${money(100)}, "taxes": [ $tax ] }
            },
            {
              "type": "ITEM_TYPE_STORNO_MARKUP",
              "stornoMarkup": { "name": "Сторно наценки", "sum": ${money(100)} }
            },
            {
              "type": "ITEM_TYPE_DISCOUNT",
              "discount": { "name": "Скидка", "sum": ${money(50)} }
            },
            {
              "type": "ITEM_TYPE_STORNO_DISCOUNT",
              "stornoDiscount": { "name": "Сторно скидки", "sum": ${money(50)} }
            },
            {
              "type": "ITEM_TYPE_STORNO_COMMODITY",
              "stornoCommodity": {
                "name": "Товар 1",
                "sectionCode": "1",
                "quantity": 1,
                "price": ${money(200)},
                "sum": ${money(200)},
                "measureUnitCode": "796",
                "taxes": [ $tax ],
                "listExciseStamp": [ "СТАМП-3" ]
              }
            }
          ],
          "payments": [ { "type": "PAYMENT_CASH", "sum": ${money(800)} } ],
          "amounts": {
            "total": ${money(800)},
            "taken": ${money(1000)},
            "change": ${money(200)},
            "markup": { "name": "Наценка по чеку", "sum": ${money(100)} }
          },
          "printedTicket": "3"
        }
    """.trimIndent()

    @Test
    fun shouldSerializeMaximalTicket() {
        val result = codec.encode(envelope(maximalTicket))

        assertTrue(result.isSuccess, "Предельный чек обязан собираться: ${errorsOf(result.exceptionOrNull())}")
    }

    @Test
    fun shouldRefuseTicketWithoutItems() {
        val result = codec.encode(
            envelope(
                """
                {
                  "operation": "OPERATION_SELL",
                  "dateTime": {
                    "date": { "year": 2024, "month": 9, "day": 1 },
                    "time": { "hour": 12, "minute": 5, "second": 0 }
                  },
                  "operator": { "code": 1, "name": "Кассир 1" },
                  "domain": { "type": "DOMAIN_TRADING" },
                  "payments": [ { "type": "PAYMENT_CASH", "sum": ${money(1000)} } ],
                  "amounts": { "total": ${money(1000)} },
                  "printedTicket": "4"
                }
                """.trimIndent()
            )
        )

        assertTrue(result.isFailure, "Чек без позиций обязан быть отвергнут")
    }

    @Test
    fun shouldRefuseUnknownTaxType() {
        val broken = maximalTicket.replace("\"taxType\": 100", "\"taxType\": 7")

        val result = codec.encode(envelope(broken))

        assertTrue(result.isFailure, "Неизвестный вид налога обязан быть отвергнут")
    }

    @Test
    fun shouldRefuseCommodityWithoutNameAndCode() {
        val broken = maximalTicket
            .replace("\"name\": \"Товар 1\",\n                \"code\": 12345,", "")

        val result = codec.encode(envelope(broken))

        assertTrue(result.isFailure, "Позиция без наименования и без кода обязана быть отвергнута")
    }
}
