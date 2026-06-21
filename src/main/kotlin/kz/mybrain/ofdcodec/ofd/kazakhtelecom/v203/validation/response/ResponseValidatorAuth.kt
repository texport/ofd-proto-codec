package kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.validation.response

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull
import kz.mybrain.ofdcodec.domain.model.CommandType
import kz.mybrain.ofdcodec.domain.model.ValidationError
import kz.mybrain.ofdcodec.domain.port.Validator
import kz.mybrain.ofdcodec.domain.validation.ValidationUtils

/**
 * Валидатор ответа для COMMAND_AUTH.
 */
class ResponseValidatorAuth : Validator {
    /**
     * Проверяет JSON и собирает все ошибки по полям без остановки на первой.
     */
    override fun validate(commandType: CommandType, json: JsonObject): List<ValidationError> {
        val errors = mutableListOf<ValidationError>()

        val result = json["result"] as? JsonObject
        if (result == null) {
            errors.add(ValidationUtils.missingField("$.payload.result"))
            return errors
        }

        ValidationUtils.requireIntInRange(
            result,
            "resultCode",
            0,
            Int.MAX_VALUE,
            "$.payload.result.resultCode",
            errors
        )

        val resultCodeValue = (result["resultCode"] as? JsonPrimitive)?.intOrNull
        if (resultCodeValue != null && resultCodeValue != 0) {
            // При ошибке сервера (resultCode != 0) блок auth может отсутствовать.
            return errors
        }

        val auth = json["auth"] as? JsonObject
        if (auth == null) {
            errors.add(ValidationUtils.missingField("$.payload.auth"))
            return errors
        }

        ValidationUtils.requireNonBlankString(auth, "result", "$.payload.auth.result", errors)

        val authResult = (auth["result"] as? JsonPrimitive)?.content
        if (authResult == "RESULT_TYPE_OK") {
            ValidationUtils.requireIntInRange(
                auth,
                "operatorCode",
                0,
                Int.MAX_VALUE,
                "$.payload.auth.operatorCode",
                errors
            )
            ValidationUtils.requireNonBlankString(auth, "operatorName", "$.payload.auth.operatorName", errors)

            val roles = auth["roles"]
            if (roles != null) {
                if (roles !is JsonArray) {
                    errors.add(ValidationUtils.invalidType("$.payload.auth.roles"))
                } else {
                    roles.forEachIndexed { index, role ->
                        if (role !is JsonPrimitive || !role.isString) {
                            errors.add(ValidationUtils.invalidType("$.payload.auth.roles[$index]"))
                        }
                    }
                }
            }
        }

        return errors
    }
}
