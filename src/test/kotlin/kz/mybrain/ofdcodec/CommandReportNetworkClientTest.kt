package kz.mybrain.ofdcodec

import kz.kazakhtelecom.proto.v203.Message
import kz.mybrain.network.OfdEndpoint
import kz.mybrain.network.OfdTcpNetworkClient
import kz.mybrain.ofdcodec.application.DefaultRegistry
import kz.mybrain.ofdcodec.application.OfdCodec
import kz.mybrain.ofdcodec.domain.model.HeaderConstants
import kz.mybrain.ofdcodec.domain.model.OfdCodecException
import kz.mybrain.ofdcodec.infrastructure.header.HeaderCodec
import kz.mybrain.ofdcodec.infrastructure.header.HeaderDecodeResult
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonPrimitive
import java.util.Base64
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class CommandReportNetworkClientTest {
    @Test
    fun shouldSendCommandReportZOverTcp() = runBlocking {
        sendAndCheck(reportType = "REPORT_Z", reqNum = TestReqNum.value(50))
    }

    @Test
    fun shouldSendCommandReportXOverTcp() = runBlocking {
        sendAndCheck(reportType = "REPORT_X", reqNum = TestReqNum.value(51))
    }

    private suspend fun sendAndCheck(reportType: String, reqNum: Int) {
        val host = System.getenv("OFD_TEST_HOST") ?: System.getProperty("OFD_TEST_HOST")
        val port = (System.getenv("OFD_TEST_PORT") ?: System.getProperty("OFD_TEST_PORT"))?.toIntOrNull()
        if (host.isNullOrBlank() || port == null) {
            println(
                "RU: Пропуск теста. Укажите OFD_TEST_HOST и OFD_TEST_PORT для проверки TCP.\n" +
                    "EN: Skipping test. Set OFD_TEST_HOST and OFD_TEST_PORT to check TCP."
            )
            return
        }

        val token = TestTokenProvider.current()
        if (token == null) {
            println(
                "RU: Пропуск теста. Укажите OFD_TEST_TOKEN для проверки TCP.\n" +
                    "EN: Skipping test. Set OFD_TEST_TOKEN to check TCP."
            )
            return
        }
        val json = buildRequestJson(reportType, reqNum, token)
        println(
            "RU: Полный JSON запроса к серверу:\n${json.toString()}\n" +
                "EN: Full request JSON:\n${json.toString()}"
        )

        val codec = OfdCodec(DefaultRegistry.create())
        val encodeResult = codec.encode(json)
        assertTrue(
            encodeResult.isSuccess,
            "RU: Ожидается успешная сериализация запроса.\n" +
                "EN: Expected successful request serialization.\n" +
                formatErrors(encodeResult.exceptionOrNull())
        )
        val output = encodeResult.getOrNull()
        assertNotNull(
            output,
            "RU: Выходной JSON не должен быть пустым.\n" +
                "EN: Output JSON must not be null."
        )
        val messageBase64 = output["messageBase64"]?.jsonPrimitive?.content
        assertTrue(
            !messageBase64.isNullOrBlank(),
            "RU: Поле messageBase64 должно быть заполнено.\n" +
                "EN: Field messageBase64 must be present."
        )
        val requestBytes = Base64.getDecoder().decode(messageBase64)

        val client = OfdTcpNetworkClient()
        val responseResult = client.sendAndReceive(OfdEndpoint(host, port), requestBytes)
        assertTrue(
            responseResult.isSuccess,
            "RU: TCP-отправка завершилась ошибкой: ${responseResult.exceptionOrNull()}.\n" +
                "EN: TCP send failed: ${responseResult.exceptionOrNull()}."
        )
        val responseBytes = responseResult.getOrNull()
        assertNotNull(
            responseBytes,
            "RU: Ответ от сервера не должен быть пустым.\n" +
                "EN: Server response must not be null."
        )
        assertTrue(
            responseBytes.size >= 18,
            "RU: Ответ меньше заголовка (18 байт).\n" +
                "EN: Response is smaller than header (18 bytes)."
        )

        val responseBase64 = Base64.getEncoder().encodeToString(responseBytes)
        println(
            "RU: Ответ от сервера получен. size=${responseBytes.size}.\n" +
                "EN: Server response received. size=${responseBytes.size}."
        )
        println("RU: responseBase64 = $responseBase64\nEN: responseBase64 = $responseBase64")

        val headerResult = HeaderCodec.decode(responseBytes)
        if (headerResult is HeaderDecodeResult.Success) {
            TestTokenProvider.update(headerResult.header.token)
            val payloadBytes = responseBytes.copyOfRange(
                HeaderConstants.HEADER_SIZE,
                headerResult.header.size.toInt().coerceAtMost(responseBytes.size)
            )
            val protoResponse = Message.Response.parser().parsePartialFrom(payloadBytes)
            println(
                "RU: Десериализованный proto-ответ сервера:\n${protoResponse}\n" +
                    "EN: Raw proto response from server:\n${protoResponse}"
            )
        } else {
            println(
                "RU: Не удалось разобрать заголовок ответа сервера.\n" +
                    "EN: Failed to decode server response header."
            )
        }

        val decoded = codec.decode(responseBytes)
        assertTrue(
            decoded.isSuccess,
            "RU: Не удалось декодировать ответ сервера.\n" +
                "EN: Failed to decode server response.\n" +
                formatErrors(decoded.exceptionOrNull())
        )
        val decodedJson = decoded.getOrNull()
        assertNotNull(
            decodedJson,
            "RU: Декодированный JSON не должен быть пустым.\n" +
                "EN: Decoded JSON must not be null."
        )
        println(
            "RU: Декодированный ответ сервера:\n${decodedJson.toString()}\n" +
                "EN: Decoded server response:\n${decodedJson.toString()}"
        )
    }

    private fun buildRequestJson(reportType: String, reqNum: Int, token: Long): kotlinx.serialization.json.JsonElement {
        val shiftTimesBlock = if (reportType == "REPORT_Z") {
            """
                    "openShiftTime": {
                      "date": { "year": 2024, "month": 9, "day": 1 },
                      "time": { "hour": 9, "minute": 0, "second": 0 }
                    },
                    "closeShiftTime": {
                      "date": { "year": 2024, "month": 9, "day": 1 },
                      "time": { "hour": 12, "minute": 0, "second": 0 }
                    },
            """.trimIndent()
        } else {
            """
                    "openShiftTime": {
                      "date": { "year": 2024, "month": 9, "day": 1 },
                      "time": { "hour": 9, "minute": 0, "second": 0 }
                    },
            """.trimIndent()
        }
        return Json.parseToJsonElement(
            """
            {
              "ofdId": "kazakhtelecom",
              "protocolVersion": "203",
              "messageType": "REQUEST",
              "commandType": "COMMAND_REPORT",
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
                "report": {
                  "reportType": "$reportType",
                  "dateTime": {
                    "date": { "year": 2024, "month": 9, "day": 1 },
                    "time": { "hour": 11, "minute": 0, "second": 0 }
                  },
                  "isOffline": false,
                  "zxReport": {
                    "dateTime": {
                      "date": { "year": 2024, "month": 9, "day": 1 },
                      "time": { "hour": 11, "minute": 0, "second": 0 }
                    },
                    $shiftTimesBlock
                    "shiftNumber": 1,
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
    }

    private fun formatErrors(exception: Throwable?): String {
        val ex = exception as? OfdCodecException ?: return ""
        val details = ex.errors.joinToString(separator = "\n") {
            "RU: ${it.messageRu} | EN: ${it.messageEn} | path=${it.path} | code=${it.code}"
        }
        return if (details.isBlank()) "" else "Errors:\n$details"
    }
}
