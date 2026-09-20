package kz.mybrain.ofdcodec

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kz.mybrain.ofdcodec.domain.model.CommandType
import kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.validation.response.ResponseValidatorAuth
import kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.validation.response.ResponseValidatorTicket
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Защитные ветви проверки ответа ОФД.
 *
 * Разобранный ответ приходит из protobuf и по форме всегда правилен, поэтому
 * эти ветви через decode недостижимы. Но проверка стоит именно на случай
 * ответа неверной формы, и её поведение должно быть зафиксировано.
 */
class ResponseValidatorGuardsTest {

    private fun json(body: String): JsonObject = Json.parseToJsonElement(body) as JsonObject

    private fun assertRefuses(errors: List<Any>, what: String) =
        assertTrue(errors.isNotEmpty(), "$what обязан быть отвергнут")

    @Test
    fun shouldRefuseAuthResponseWithRolesOfAWrongShape() {
        val validator = ResponseValidatorAuth()

        assertRefuses(
            validator.validate(
                CommandType.COMMAND_AUTH,
                json("""{ "result": { "resultCode": 0 }, "auth": { "result": "RESULT_TYPE_OK", "operatorCode": 1, "operatorName": "Кассир", "roles": "не список" } }""")
            ),
            "Роли не списком"
        )
        assertRefuses(
            validator.validate(
                CommandType.COMMAND_AUTH,
                json("""{ "result": { "resultCode": 0 }, "auth": { "result": "RESULT_TYPE_OK", "operatorCode": 1, "operatorName": "Кассир", "roles": [ 7 ] } }""")
            ),
            "Роль числом вместо названия"
        )
    }

    @Test
    fun shouldRefuseAuthResponseWithoutResultBlock() {
        assertRefuses(
            ResponseValidatorAuth().validate(CommandType.COMMAND_AUTH, json("{}")),
            "Ответ авторизации без результата"
        )
    }

    @Test
    fun shouldRefuseTicketResponseOfAWrongShape() {
        val validator = ResponseValidatorTicket()

        assertRefuses(
            validator.validate(
                CommandType.COMMAND_TICKET,
                json("""{ "result": { "resultCode": 0 }, "ticket": "не объект" }""")
            ),
            "Блок чека не объектом"
        )
        assertRefuses(
            validator.validate(
                CommandType.COMMAND_TICKET,
                json("""{ "result": { "resultCode": 0 }, "ticket": { "ticketNumber": "1" }, "service": "не объект" }""")
            ),
            "Служебная часть не объектом"
        )
    }
}
