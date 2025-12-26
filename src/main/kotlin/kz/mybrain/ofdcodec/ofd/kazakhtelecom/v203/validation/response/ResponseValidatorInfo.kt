package kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.validation.response

import kz.mybrain.ofdcodec.domain.model.CommandType
import kz.mybrain.ofdcodec.domain.model.ValidationError
import kz.mybrain.ofdcodec.domain.port.Validator
import kz.mybrain.ofdcodec.domain.validation.ValidationUtils
import kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.validation.response.service.ServiceResponseValidator
import kotlinx.serialization.json.JsonObject

/**
 * Валидатор ответа для COMMAND_INFO.
 */
class ResponseValidatorInfo : Validator {
    private val serviceValidator = ServiceResponseValidator()

    /**
     * Проверяет JSON и собирает все ошибки по полям без остановки на первой.
     * В ответе обязателен result, service валидируется при наличии.
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

        val service = json["service"]
        if (service != null) {
            val serviceObject = service as? JsonObject
            if (serviceObject == null) {
                errors.add(ValidationUtils.invalidType("$.payload.service"))
            } else {
                errors.addAll(serviceValidator.validate(serviceObject, "$.payload.service"))
            }
        }

        return errors
    }
}
