package kz.mybrain.ofdcodec

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kz.mybrain.ofdcodec.application.DefaultRegistry
import kz.mybrain.ofdcodec.application.OfdCodec
import kz.mybrain.ofdcodec.domain.model.OfdCodecException
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Полный Z-отчёт по протоколу 2.0.4.
 *
 * Отчёт смены — единственное место, где сходятся все итоги кассы, поэтому
 * он собирается со всеми разделами сразу: секции, операции, скидки, наценки,
 * налоги, необнуляемые суммы, операции по чекам с разбивкой по видам оплаты
 * и движение денег в ящике.
 */
class BfdV204FullReportTest {

    private val codec = OfdCodec(DefaultRegistry.create())

    private fun money(bills: Long, coins: Int = 0) = """{ "bills": $bills, "coins": $coins }"""

    private fun report(payments: String): JsonElement = Json.parseToJsonElement(
        """
        {
          "ofdId": "bfd",
          "protocolVersion": "204",
          "messageType": "REQUEST",
          "commandType": "COMMAND_REPORT",
          "header": { "deviceId": 201873, "token": 208627316, "reqNum": 7 },
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
            "report": {
              "reportType": "REPORT_Z",
              "dateTime": {
                "date": { "year": 2024, "month": 9, "day": 1 },
                "time": { "hour": 23, "minute": 0, "second": 0 }
              },
              "zxReport": {
                "dateTime": {
                  "date": { "year": 2024, "month": 9, "day": 1 },
                  "time": { "hour": 23, "minute": 0, "second": 0 }
                },
                "openShiftTime": {
                  "date": { "year": 2024, "month": 9, "day": 1 },
                  "time": { "hour": 8, "minute": 0, "second": 0 }
                },
                "closeShiftTime": {
                  "date": { "year": 2024, "month": 9, "day": 1 },
                  "time": { "hour": 23, "minute": 0, "second": 0 }
                },
                "shiftNumber": 12,
                "sections": [
                  {
                    "sectionCode": "1",
                    "operations": [
                      { "operation": "OPERATION_SELL", "count": 3, "sum": ${money(9000)} }
                    ]
                  }
                ],
                "operations": [
                  { "operation": "OPERATION_SELL", "count": 3, "sum": ${money(9000)} },
                  { "operation": "OPERATION_SELL_RETURN", "count": 1, "sum": ${money(1000)} }
                ],
                "discounts": [
                  { "operation": "OPERATION_SELL", "count": 1, "sum": ${money(200)} }
                ],
                "markups": [
                  { "operation": "OPERATION_SELL", "count": 1, "sum": ${money(100)} }
                ],
                "totalResult": [
                  { "operation": "OPERATION_SELL", "count": 3, "sum": ${money(8900)} }
                ],
                "taxes": [
                  {
                    "taxType": 100,
                    "percent": 16000,
                    "operations": [
                      {
                        "operation": "OPERATION_SELL",
                        "turnover": ${money(8900)},
                        "sum": ${money(1227)},
                        "turnoverWithoutTax": ${money(7673)}
                      }
                    ]
                  }
                ],
                "startShiftNonNullableSums": [
                  { "operation": "OPERATION_SELL", "sum": ${money(100000)} }
                ],
                "nonNullableSums": [
                  { "operation": "OPERATION_SELL", "sum": ${money(108900)} }
                ],
                "ticketOperations": [
                  {
                    "operation": "OPERATION_SELL",
                    "ticketsTotalCount": 3,
                    "ticketsCount": 3,
                    "ticketsSum": ${money(8900)},
                    "payments": [$payments],
                    "offlineCount": 0,
                    "discountSum": ${money(200)},
                    "markupSum": ${money(100)},
                    "changeSum": ${money(0)}
                  }
                ],
                "moneyPlacements": [
                  {
                    "operation": "MONEY_PLACEMENT_DEPOSIT",
                    "operationsTotalCount": 1,
                    "operationsCount": 1,
                    "operationsSum": ${money(5000)},
                    "offlineCount": 0
                  }
                ],
                "cashSum": ${money(13900)},
                "revenue": { "sum": ${money(8900)}, "isNegative": false }
              }
            }
          }
        }
        """.trimIndent()
    )

    private fun errorsOf(exception: Throwable?): String =
        (exception as? OfdCodecException)?.errors?.joinToString("\n") {
            "${it.messageRu} | path=${it.path} | code=${it.code}"
        } ?: exception?.message.orEmpty()

    @Test
    fun shouldSerializeFullZReport() {
        val payments = """{ "payment": "PAYMENT_CASH", "sum": ${money(8900)}, "count": 3 }"""

        val result = codec.encode(report(payments))

        assertTrue(result.isSuccess, "Полный Z-отчёт обязан собираться: ${errorsOf(result.exceptionOrNull())}")
    }

    @Test
    fun shouldDropEmptyLineOfAPaymentTypeAbsentIn204() {
        // Пустая строка по отменённому виду оплаты ничего не сообщает
        // и отчёт из-за неё падать не должен.
        val payments = """
            { "payment": "PAYMENT_CASH", "sum": ${money(8900)}, "count": 3 },
            { "payment": "PAYMENT_CREDIT", "sum": ${money(0)}, "count": 0 }
        """.trimIndent()

        val result = codec.encode(report(payments))

        assertTrue(result.isSuccess, "Пустая строка кредита не должна ломать отчёт: ${errorsOf(result.exceptionOrNull())}")
    }

    @Test
    fun shouldRefuseTurnoverOnAPaymentTypeAbsentIn204() {
        // А вот строка с оборотом — это деньги. Молча выбросить её значит
        // потерять их в отчёте смены, поэтому отчёт обязан быть отвергнут.
        val payments = """
            { "payment": "PAYMENT_CASH", "sum": ${money(8000)}, "count": 2 },
            { "payment": "PAYMENT_CREDIT", "sum": ${money(900)}, "count": 1 }
        """.trimIndent()

        val result = codec.encode(report(payments))

        assertTrue(result.isFailure, "Оборот по несуществующему в 2.0.4 виду оплаты обязан быть отвергнут")
        val reason = errorsOf(result.exceptionOrNull())
        assertTrue(
            reason.contains("PAYMENT_CREDIT"),
            "Отказ обязан называть вид оплаты, а не общую причину. Получено: " + reason
        )
    }
}
