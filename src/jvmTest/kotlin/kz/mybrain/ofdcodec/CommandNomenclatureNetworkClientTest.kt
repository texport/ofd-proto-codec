package kz.mybrain.ofdcodec

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonPrimitive
import kz.kazakhtelecom.proto.v203.*
import kz.mybrain.network.OfdEndpoint
import kz.mybrain.network.OfdTcpNetworkClient
import kz.mybrain.ofdcodec.application.DefaultRegistry
import kz.mybrain.ofdcodec.application.OfdCodec
import kz.mybrain.ofdcodec.domain.model.HeaderConstants
import kz.mybrain.ofdcodec.domain.model.OfdCodecException
import kz.mybrain.ofdcodec.infrastructure.header.HeaderCodec
import kz.mybrain.ofdcodec.infrastructure.header.HeaderDecodeResult
import java.util.Base64
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class CommandNomenclatureNetworkClientTest {
    @Test
    fun shouldSendCommandNomenclatureCurrentVersionOnly() = runBlocking {
        sendAndCheck(
            currentVersion = 0,
            barcode = null,
            reqNum = TestReqNum.value(40)
        )
    }

    @Test
    fun shouldSendCommandNomenclatureWithBarcode() = runBlocking {
        sendAndCheck(
            currentVersion = 1,
            barcode = "0200091530572",
            reqNum = TestReqNum.value(41)
        )
    }

    @Test
    fun shouldSendCommandNomenclatureWith5449000176431Barcode() = runBlocking {
        sendAndCheck(
            currentVersion = 1,
            barcode = "5449000176431",
            reqNum = TestReqNum.value(42)
        )
    }

    @Test
    fun shouldSendCommandNomenclatureWithMarkingCode() = runBlocking {
        sendAndCheck(
            currentVersion = 1,
            barcode = "0104820024700016215N39N41355416",
            reqNum = TestReqNum.value(43)
        )
    }

    private suspend fun sendAndCheck(currentVersion: Int, barcode: String?, reqNum: Int) {
        val host = System.getenv("OFD_TEST_HOST") ?: System.getProperty("OFD_TEST_HOST")
        val port = (System.getenv("OFD_TEST_PORT") ?: System.getProperty("OFD_TEST_PORT"))?.toIntOrNull()
        val token = TestTokenProvider.current()
        if (host.isNullOrBlank() || port == null || token == null) {
            println(
                "RU: Пропуск теста. Укажите OFD_TEST_HOST, OFD_TEST_PORT и OFD_TEST_TOKEN для проверки TCP.\n" +
                    "EN: Skipping test. Set OFD_TEST_HOST, OFD_TEST_PORT and OFD_TEST_TOKEN to check TCP."
            )
            return
        }

        val barcodePart = if (barcode == null) "" else ", \"barcode\": \"$barcode\""
        val json = Json.parseToJsonElement(
            """
            {
              "ofdId": "kazakhtelecom",
              "protocolVersion": "203",
              "messageType": "REQUEST",
              "commandType": "COMMAND_NOMENCLATURE",
              "header": {
                "deviceId": 203605,
                "token": $token,
                "reqNum": $reqNum
              },
              "payload": {
                "service": {
                  "getRegInfo": true,
                  "offlinePeriod": {
                    "beginTime": {
                      "date": { "year": 2026, "month": 7, "day": 8 },
                      "time": { "hour": 10, "minute": 30, "second": 0 }
                    },
                    "endTime": {
                      "date": { "year": 2026, "month": 7, "day": 8 },
                      "time": { "hour": 10, "minute": 40, "second": 0 }
                    }
                  },
                  "securityStats": {
                    "geoPosition": {
                      "latitude": 511629,
                      "longitude": 714463,
                      "source": "CELL"
                    }
                  },
                  "regInfo": {
                    "kkm": {
                      "fnsKkmId": "620300013016",
                      "serialNumber": "34523452345345345",
                      "kkmId": "203605"
                    },
                    "org": {
                      "title": "ИП МИЧКА ПАВЕЛ АНДРЕЕВИЧ",
                      "address": "г. Астана, р-н Сарыарка,  г. Астана, пр. Бөгенбай Батыр д. 44",
                      "addressKz": "Республика Қазақстан, қ. Астана, ауд. Сарыарқа, пр. Бөгенбай Батыр д. 44",
                      "inn": "960624350642",
                      "okved": "47110"
                    }
                  }
                },
                "nomenclature": {
                  "currentVersion": $currentVersion$barcodePart
                }
              }
            }
            """.trimIndent()
        )
        println(
            "RU: Полный JSON запроса к серверу:\n$json\n" +
                "EN: Full request JSON:\n$json"
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
            val protoResponse = Response.ADAPTER.decode(payloadBytes)
            println(
                "RU: Десериализованный proto-ответ сервера:\n${protoResponse}\n" +
                    "EN: Raw proto response from server:\n$protoResponse"
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
            "RU: Декодированный ответ сервера:\n$decodedJson\n" +
                "EN: Decoded server response:\n$decodedJson"
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
