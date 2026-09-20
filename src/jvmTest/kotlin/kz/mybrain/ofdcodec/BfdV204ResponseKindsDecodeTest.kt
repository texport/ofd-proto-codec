package kz.mybrain.ofdcodec

import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kz.bfd.proto.v204.AuthResponse
import kz.bfd.proto.v204.CommandTypeEnum
import kz.bfd.proto.v204.Date
import kz.bfd.proto.v204.DateTime
import kz.bfd.proto.v204.Money
import kz.bfd.proto.v204.QRResponse
import kz.bfd.proto.v204.ReportResponse
import kz.bfd.proto.v204.ReportTypeEnum
import kz.bfd.proto.v204.Response
import kz.bfd.proto.v204.Result
import kz.bfd.proto.v204.StatusTypeEnum
import kz.bfd.proto.v204.Time
import kz.bfd.proto.v204.UserRoleEnum
import kz.bfd.proto.v204.ZXReport
import kz.mybrain.ofdcodec.application.DefaultRegistry
import kz.mybrain.ofdcodec.application.OfdCodec
import kz.mybrain.ofdcodec.domain.model.MessageHeader
import kz.mybrain.ofdcodec.infrastructure.header.HeaderCodec
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Разбор всех видов ответа ОФД по протоколу 2.0.4.
 *
 * Каждый ответ собирается протобуфом и подаётся декодеру байтами: проверяется
 * именно разбор, без сети и без живого стенда.
 */
class BfdV204ResponseKindsDecodeTest {

    private val codec = OfdCodec(DefaultRegistry.create())

    private fun frame(response: Response): ByteArray {
        val payload = Response.ADAPTER.encode(response)
        val header = MessageHeader(
            appCode = 0, protocolVersion = 204, size = 0,
            deviceId = 201873L, token = 4_079_722_096L, reqNum = 5
        )
        return HeaderCodec.encode(header, payload.size) + payload
    }

    private fun moment(hour: Int) = DateTime(
        date = Date(year = 2024, month = 9, day = 1),
        time = Time(hour = hour, minute = 0, second = 0)
    )

    private fun money(bills: Long) = Money(bills = bills, coins = 0)

    private fun zxReport() = ZXReport(
        date_time = moment(23),
        shift_number = 12,
        cash_sum = money(13_900),
        revenue = ZXReport.Revenue(sum = money(8_900), is_negative = false),
        open_shift_time = moment(8),
        close_shift_time = moment(23),
        checksum = "DEADBEEF"
    )

    @Test
    fun shouldDecodeReportResponse() {
        val response = Response(
            command = CommandTypeEnum.COMMAND_REPORT,
            result = Result(result_code = 0),
            report = ReportResponse(report = ReportTypeEnum.REPORT_Z, zx_report = zxReport())
        )

        val decoded = codec.decode(frame(response))

        assertTrue(decoded.isSuccess, "Ответ на отчёт обязан разбираться: ${decoded.exceptionOrNull()}")
        val report = decoded.getOrNull()!!["payload"]?.jsonObject?.get("report")?.jsonObject
        assertEquals(
            12,
            report?.get("zxReport")?.jsonObject?.get("shiftNumber")?.jsonPrimitive?.content?.toInt()
        )
    }

    @Test
    fun shouldDecodeAuthResponse() {
        val response = Response(
            command = CommandTypeEnum.COMMAND_AUTH,
            result = Result(result_code = 0),
            auth = AuthResponse(
                result = AuthResponse.ResultTypeEnum.RESULT_TYPE_OK,
                operator_code = 1,
                operator_name = "Кассир 1",
                roles = listOf(UserRoleEnum.USER_ROLE_PAYMASTER)
            )
        )

        val decoded = codec.decode(frame(response))

        assertTrue(decoded.isSuccess, "Ответ авторизации обязан разбираться: ${decoded.exceptionOrNull()}")
        val auth = decoded.getOrNull()!!["payload"]?.jsonObject?.get("auth")?.jsonObject
        assertEquals("Кассир 1", auth?.get("operatorName")?.jsonPrimitive?.content)
    }

    @Test
    fun shouldRefuseUnsupportedQrCommandByName() {
        // Команды QR протокол 2.0.4 описывает, но кодек их не поддерживает
        // и касса их не использует. Важно, что отказ называет команду,
        // а не выглядит сбоем разбора.
        val response = Response(
            command = CommandTypeEnum.COMMAND_QR_GET_STATUS,
            result = Result(result_code = 0),
            qr = QRResponse(qr_code = "QR-1", status = StatusTypeEnum.STATUS_SCANNED)
        )

        val decoded = codec.decode(frame(response))

        assertTrue(decoded.isFailure, "Неподдерживаемая команда не может считаться разобранной")
        assertTrue(
            decoded.exceptionOrNull()?.message?.contains("COMMAND_QR_GET_STATUS") == true,
            "Отказ обязан называть команду: ${decoded.exceptionOrNull()?.message}"
        )
    }

    @Test
    fun shouldDecodeTokenAboveSignedIntRange() {
        // Токен объявлен как uint32, и стенд действительно выдаёт значения
        // выше 2147483647. Заголовок обязан донести такое значение целым.
        val response = Response(
            command = CommandTypeEnum.COMMAND_SYSTEM,
            result = Result(result_code = 0)
        )

        val decoded = codec.decode(frame(response))

        assertTrue(decoded.isSuccess)
        val header = decoded.getOrNull()!!["header"]?.jsonObject
        assertEquals(
            4_079_722_096L,
            header?.get("token")?.jsonPrimitive?.content?.toLong()
        )
    }

    @Test
    fun shouldDecodeAuthRefusalWithoutAuthBlock() {
        // При отказе сервера блок auth вправе отсутствовать: проверка ответа
        // обязана это допускать, иначе касса не увидит причину отказа.
        val response = Response(
            command = CommandTypeEnum.COMMAND_AUTH,
            result = Result(result_code = 13, result_text = "Неверные входные данные")
        )

        val decoded = codec.decode(frame(response))

        assertTrue(decoded.isSuccess, "Отказ авторизации обязан разбираться: ${decoded.exceptionOrNull()}")
    }

    @Test
    fun shouldRefuseSuccessfulAuthWithoutAuthBlock() {
        // А вот успех без блока auth — это ответ ни о ком.
        val response = Response(
            command = CommandTypeEnum.COMMAND_AUTH,
            result = Result(result_code = 0)
        )

        assertTrue(codec.decode(frame(response)).isFailure, "Успех без данных кассира недопустим")
    }

    @Test
    fun shouldDecodeAuthRefusalOfTheOperator() {
        // Сервер ответил успешно, но кассира не признал.
        val response = Response(
            command = CommandTypeEnum.COMMAND_AUTH,
            result = Result(result_code = 0),
            auth = AuthResponse(result = AuthResponse.ResultTypeEnum.RESULT_TYPE_INVALID_LOGIN_PASSWORD)
        )

        val decoded = codec.decode(frame(response))

        assertTrue(decoded.isSuccess, "Отказ по кассиру обязан разбираться: ${decoded.exceptionOrNull()}")
    }
}
