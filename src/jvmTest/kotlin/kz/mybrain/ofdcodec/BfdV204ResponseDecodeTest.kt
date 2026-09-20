package kz.mybrain.ofdcodec

import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kz.bfd.proto.v204.CommandTypeEnum
import kz.bfd.proto.v204.Response
import kz.bfd.proto.v204.Result
import kz.bfd.proto.v204.TicketResponse
import kz.mybrain.ofdcodec.application.DefaultRegistry
import kz.mybrain.ofdcodec.application.OfdCodec
import kz.mybrain.ofdcodec.domain.model.MessageHeader
import kz.mybrain.ofdcodec.infrastructure.header.HeaderCodec
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Разбор ответа ОФД по протоколу 2.0.4.
 *
 * Ответ собирается протобуфом прямо в тесте и подаётся декодеру байтами:
 * так проверяется именно разбор, без живого стенда и без сети.
 */
class BfdV204ResponseDecodeTest {

    private val codec = OfdCodec(DefaultRegistry.create())

    private fun frame(response: Response, deviceId: Long = 201873L, token: Long = 208627316L): ByteArray {
        val payload = Response.ADAPTER.encode(response)
        val header = MessageHeader(
            appCode = 0,
            protocolVersion = 204,
            size = 0,
            deviceId = deviceId,
            token = token,
            reqNum = 1
        )
        return HeaderCodec.encode(header, payload.size) + payload
    }

    @Test
    fun shouldDecodeSuccessfulTicketResponse() {
        val response = Response(
            command = CommandTypeEnum.COMMAND_TICKET,
            result = Result(result_code = 0),
            ticket = TicketResponse(ticket_number = "1234567890")
        )

        val decoded = codec.decode(frame(response))

        assertTrue(decoded.isSuccess, "Ответ обязан разбираться: ${decoded.exceptionOrNull()}")
        val json = decoded.getOrNull()!!
        val payload = json["payload"]?.jsonObject
        assertEquals(
            "1234567890",
            payload?.get("ticket")?.jsonObject?.get("ticketNumber")?.jsonPrimitive?.content
        )
    }

    @Test
    fun shouldDecodeRefusalWithItsCodeAndText() {
        // Отказ обязан доходить до кассы кодом и текстом: по ним она решает,
        // блокироваться, повторять или уходить в автономный режим.
        val response = Response(
            command = CommandTypeEnum.COMMAND_TICKET,
            result = Result(result_code = 18, result_text = "Касса снята с учета")
        )

        val decoded = codec.decode(frame(response))

        assertTrue(decoded.isSuccess, "Отказ — это тоже разбираемый ответ")
        val result = decoded.getOrNull()!!["payload"]?.jsonObject?.get("result")?.jsonObject
        assertEquals(18, result?.get("resultCode")?.jsonPrimitive?.content?.toInt())
        assertEquals("Касса снята с учета", result?.get("resultText")?.jsonPrimitive?.content)
    }

    @Test
    fun shouldRefuseTruncatedFrame() {
        val response = Response(
            command = CommandTypeEnum.COMMAND_TICKET,
            result = Result(result_code = 0)
        )
        val full = frame(response)

        val decoded = codec.decode(full.copyOf(full.size / 2))

        assertTrue(decoded.isFailure, "Обрезанный кадр не может считаться разобранным")
    }

    @Test
    fun shouldRefuseEmptyInput() {
        assertTrue(codec.decode(ByteArray(0)).isFailure)
    }
}
