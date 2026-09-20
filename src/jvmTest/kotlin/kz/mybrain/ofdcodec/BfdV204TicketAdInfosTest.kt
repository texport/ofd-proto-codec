package kz.mybrain.ofdcodec

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonPrimitive
import kz.bfd.proto.v204.Request
import kz.mybrain.ofdcodec.application.DefaultRegistry
import kz.mybrain.ofdcodec.application.OfdCodec
import okio.ByteString.Companion.decodeBase64

/**
 * Версии рекламных текстов доезжают от кассы до протокола.
 *
 * Сервер отдаёт только те тексты, что новее присланных версий. Пока список
 * собирался пустым, сравнивать было не с чем, и реклама не доходила ни до
 * одной кассы.
 */
class BfdV204TicketAdInfosTest {

    @Test
    fun ticketAdInfosReachTheWire() {
        val codec = OfdCodec(DefaultRegistry.create())

        val result = codec.encode(Json.parseToJsonElement(request()))

        assertTrue(
            result.isSuccess,
            "RU: Ожидается успешная сериализация.\nEN: Expected successful serialization."
        )
        // Разбираем то, что уйдёт в сокет: заголовок кадра и следом запрос.
        val frame = result.getOrNull()!!["messageBase64"]!!.jsonPrimitive.content
            .decodeBase64()!!.toByteArray()
        val request = Request.ADAPTER.decode(frame.copyOfRange(HEADER_SIZE, frame.size))

        val infos = request.service!!.ticket_ad_infos
        assertEquals(2, infos.size)
        assertEquals("TICKET_AD_OFD", infos[0].type.name)
        assertEquals(17L, infos[0].version)
        assertEquals("TICKET_AD_ORG", infos[1].type.name)
        assertEquals(0L, infos[1].version)
    }

    @Test
    fun anElementThatIsNotAnObjectIsRefused() {
        // Кассе, приславшей вместо объекта строку, отвечаем отказом:
        // молча пропустить значит потерять версию и снова остаться без рекламы.
        val codec = OfdCodec(DefaultRegistry.create())

        val result = codec.encode(Json.parseToJsonElement(request(ads = """[ "not-an-object" ]""")))

        assertTrue(result.isFailure, "RU: Ожидается отказ.\nEN: Expected failure.")
    }

    @Test
    fun anUnknownAdTypeIsRefused() {
        val codec = OfdCodec(DefaultRegistry.create())

        val result = codec.encode(
            Json.parseToJsonElement(request(ads = """[ { "type": "TICKET_AD_SOMETHING", "version": 1 } ]"""))
        )

        assertTrue(result.isFailure, "RU: Ожидается отказ.\nEN: Expected failure.")
    }

    @Test
    fun ticketAdInfosReachTheWireOn203() {
        // Стенд БФД говорит по 203: профиль другой, и там список собирался
        // пустым отдельно от 204.
        val codec = OfdCodec(DefaultRegistry.create())

        val result = codec.encode(Json.parseToJsonElement(request().replace("\"204\"", "\"203\"")))

        assertTrue(
            result.isSuccess,
            "RU: Ожидается успешная сериализация.\nEN: Expected successful serialization."
        )
        val frame = result.getOrNull()!!["messageBase64"]!!.jsonPrimitive.content
            .decodeBase64()!!.toByteArray()
        val request = kz.kazakhtelecom.proto.v203.Request.ADAPTER
            .decode(frame.copyOfRange(HEADER_SIZE, frame.size))
        val infos = request.service!!.ticket_ad_infos
        assertEquals(2, infos.size)
        assertEquals("TICKET_AD_OFD", infos[0].type.name)
        assertEquals(17L, infos[0].version)
    }

    @Test
    fun anElementThatIsNotAnObjectIsRefusedOn203() {
        val codec = OfdCodec(DefaultRegistry.create())

        val result = codec.encode(
            Json.parseToJsonElement(request(ads = """[ "not-an-object" ]""").replace("\"204\"", "\"203\""))
        )

        assertTrue(result.isFailure, "RU: Ожидается отказ.\nEN: Expected failure.")
    }

    @Test
    fun anUnknownAdTypeIsRefusedOn203() {
        val codec = OfdCodec(DefaultRegistry.create())

        val result = codec.encode(
            Json.parseToJsonElement(
                request(ads = """[ { "type": "TICKET_AD_SOMETHING", "version": 1 } ]""")
                    .replace("\"204\"", "\"203\"")
            )
        )

        assertTrue(result.isFailure, "RU: Ожидается отказ.\nEN: Expected failure.")
    }

    private fun request(): String = request(
        ads = """[ { "type": "TICKET_AD_OFD", "version": 17 }, { "type": "TICKET_AD_ORG", "version": 0 } ]"""
    )

    private fun request(ads: String): String = """
        {
          "ofdId": "bfd",
          "protocolVersion": "204",
          "messageType": "REQUEST",
          "commandType": "COMMAND_SYSTEM",
          "header": { "deviceId": 201873, "token": 208627316, "reqNum": 1 },
          "payload": {
            "service": {
              "getRegInfo": true,
              "ticketAdInfos": $ads,
              "offlinePeriod": {
                "beginTime": {
                  "date": { "year": 2026, "month": 9, "day": 2 },
                  "time": { "hour": 10, "minute": 30, "second": 0 }
                },
                "endTime": {
                  "date": { "year": 2026, "month": 9, "day": 2 },
                  "time": { "hour": 10, "minute": 40, "second": 0 }
                }
              },
              "securityStats": {
                "geoPosition": { "latitude": 432156, "longitude": 765432, "source": "CELL" }
              },
              "regInfo": {
                "kkm": {
                  "fnsKkmId": "KGD-2000302",
                  "serialNumber": "KZT26E2C509A200",
                  "kkmId": "2000302"
                },
                "org": {
                  "title": "ТОО «Сарыарқа Сауда»",
                  "address": "Алматы, пр. Абая, 150",
                  "addressKz": "Алматы қаласы, Абай даңғылы, 150",
                  "inn": "123456789012",
                  "okved": "47111"
                }
              }
            }
          }
        }
    """.trimIndent()
}

private const val HEADER_SIZE = 18
