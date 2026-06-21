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

class CommandCloseShiftSerializationTest {
    @Test
    fun shouldSerializeCommandCloseShiftRequest() {
        val token = TestTokenProvider.current() ?: 208627316L
        val json = Json.parseToJsonElement(
            """
            {
              "ofdId": "kazakhtelecom",
              "protocolVersion": "203",
              "messageType": "REQUEST",
              "commandType": "COMMAND_CLOSE_SHIFT",
              "header": {
                "deviceId": 201873,
                "token": $token,
                "reqNum": 1
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
                "closeShift": {
                  "closeTime": {
                    "date": { "year": 2024, "month": 9, "day": 1 },
                    "time": { "hour": 12, "minute": 0, "second": 0 }
                  },
                  "isOffline": false,
                  "frShiftNumber": 12,
                  "withdrawMoney": false,
                  "printedDocumentNumber": 1001,
                  "operator": {
                    "code": 1,
                    "name": "Operator 1"
                  },
                  "zReport": {
                    "dateTime": {
                      "date": { "year": 2024, "month": 9, "day": 1 },
                      "time": { "hour": 11, "minute": 0, "second": 0 }
                    },
                    "openShiftTime": {
                      "date": { "year": 2024, "month": 9, "day": 1 },
                      "time": { "hour": 9, "minute": 0, "second": 0 }
                    },
                    "closeShiftTime": {
                      "date": { "year": 2024, "month": 9, "day": 1 },
                      "time": { "hour": 12, "minute": 0, "second": 0 }
                    },
                    "shiftNumber": 12,
                    "cashSum": { "bills": 10000, "coins": 0 },
                    "revenue": {
                      "sum": { "bills": 10000, "coins": 0 },
                      "isNegative": false
                    }
                  }
                }
              }
            }
            """.trimIndent()
        )

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

    private fun formatErrors(exception: Throwable?): String {
        val ex = exception as? OfdCodecException ?: return ""
        val details = ex.errors.joinToString(separator = "\n") {
            "RU: ${it.messageRu} | KK: ${it.messageKk} | EN: ${it.messageEn} | path=${it.path} | code=${it.code}"
        }
        return if (details.isBlank()) "" else "Errors:\n$details"
    }
}
