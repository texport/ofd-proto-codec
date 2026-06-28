package kz.mybrain.ofdcodec

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kz.mybrain.ofdcodec.application.DefaultRegistry
import kz.mybrain.ofdcodec.application.OfdCodec
import kz.mybrain.ofdcodec.domain.model.OfdCodecException
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class CommandMoneyPlacementSerializationTest {
    @Test
    fun shouldSerializeCommandMoneyPlacementDeposit() {
        val json = buildRequestJson(
            operation = "MONEY_PLACEMENT_DEPOSIT",
            bills = 1000,
            coins = 0,
            reqNum = 1
        )
        assertSerialized(json)
    }

    @Test
    fun shouldSerializeCommandMoneyPlacementWithdrawal() {
        val json = buildRequestJson(
            operation = "MONEY_PLACEMENT_WITHDRAWAL",
            bills = 2500,
            coins = 50,
            reqNum = 2
        )
        assertSerialized(json)
    }

    private fun assertSerialized(json: kotlinx.serialization.json.JsonElement) {
        val codec = OfdCodec(DefaultRegistry.create())
        val result = codec.encode(json)

        assertTrue(
            result.isSuccess,
            "RU: Ожидается успешная сериализация запроса.\n" +
                "EN: Expected successful request serialization.\n" +
                formatErrors(result.exceptionOrNull())
        )
        val output = result.getOrNull()
        assertNotNull(
            output,
            "RU: Выходной JSON не должен быть пустым.\n" +
                "EN: Output JSON must not be null."
        )
        println(
            "RU: Входной JSON для сериализации:\n${json}\n" +
                "EN: Input JSON for serialization:\n$json"
        )
        val size = output["size"]?.jsonPrimitive?.longOrNull
        val messageBase64 = output["messageBase64"]?.jsonPrimitive?.content
        assertTrue(
            size != null && size > 0,
            "RU: Поле size должно быть положительным числом.\n" +
                "EN: Field size must be a positive number."
        )
        assertTrue(
            !messageBase64.isNullOrBlank(),
            "RU: Поле messageBase64 должно быть заполнено.\n" +
                "EN: Field messageBase64 must be present."
        )
        println(
            "RU: Сериализованное сообщение готово к отправке.\n" +
                "EN: Serialized message is ready for sending."
        )
        println("RU: size = $size байт\nEN: size = $size bytes")
        println("RU: messageBase64 = $messageBase64\nEN: messageBase64 = $messageBase64")
    }

    private fun buildRequestJson(
        operation: String,
        bills: Long,
        coins: Int,
        reqNum: Int
    ): kotlinx.serialization.json.JsonElement {
        val token = TestTokenProvider.current() ?: 208627316L
        return Json.parseToJsonElement(
            """
            {
              "ofdId": "kazakhtelecom",
              "protocolVersion": "203",
              "messageType": "REQUEST",
              "commandType": "COMMAND_MONEY_PLACEMENT",
              "header": {
                "deviceId": 201873,
                "token": $token,
                "reqNum": $reqNum
              },
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
                    "geoPosition": {
                      "latitude": 432156,
                      "longitude": 765432,
                      "source": "CELL"
                    }
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
                      "addressKz": "Республика Қазақстан, обл. Павлодарская, қ. Екібастұз, Ауэзова 88",
                      "inn": "960624350642",
                      "okved": "47301"
                    }
                  }
                },
                "moneyPlacement": {
                  "dateTime": {
                    "date": { "year": 2024, "month": 9, "day": 1 },
                    "time": { "hour": 11, "minute": 0, "second": 0 }
                  },
                  "operation": "$operation",
                  "sum": { "bills": $bills, "coins": $coins },
                  "isOffline": false,
                  "frShiftNumber": 1,
                  "printedDocumentNumber": 123456,
                  "operator": {
                    "code": 1,
                    "name": "Operator 1"
                  }
                }
              }
            }
            """.trimIndent()
        )
    }

    private fun formatErrors(exception: Throwable?): String {
        val ex = exception as? OfdCodecException ?: return ""
        val details = ex.errors.joinToString(separator = "\n") {
            "RU: ${it.messageRu} | KK: ${it.messageKk} | EN: ${it.messageEn} | path=${it.path} | code=${it.code}"
        }
        return if (details.isBlank()) "" else "Errors:\n$details"
    }
}
