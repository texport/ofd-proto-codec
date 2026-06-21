package kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.validation.request

import kotlinx.serialization.json.JsonObject
import kz.mybrain.ofdcodec.domain.model.CommandType
import kz.mybrain.ofdcodec.domain.model.ValidationError
import kz.mybrain.ofdcodec.domain.port.Validator
import kz.mybrain.ofdcodec.domain.validation.ValidationUtils
import kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.validation.request.service.ServiceRequestValidator

/**
 * Валидатор запроса для COMMAND_AUTH.
 */
class RequestValidatorAuth : Validator {
    private val serviceValidator = ServiceRequestValidator()

    /**
     * Проверяет JSON и собирает все ошибки по полям без остановки на первой.
     */
    override fun validate(commandType: CommandType, json: JsonObject): List<ValidationError> {
        val errors = mutableListOf<ValidationError>()

        val service = json["service"] as? JsonObject
        if (service == null) {
            errors.add(ValidationUtils.missingField("$.payload.service"))
            return errors
        }
        errors.addAll(serviceValidator.validate(service, "$.payload.service"))

        val auth = json["auth"] as? JsonObject
        if (auth == null) {
            errors.add(ValidationUtils.missingField("$.payload.auth"))
            return errors
        }

        ValidationUtils.requireNonBlankString(auth, "login", "$.payload.auth.login", errors)
        ValidationUtils.requireNonBlankString(auth, "password", "$.payload.auth.password", errors)

        return errors
    }
}
