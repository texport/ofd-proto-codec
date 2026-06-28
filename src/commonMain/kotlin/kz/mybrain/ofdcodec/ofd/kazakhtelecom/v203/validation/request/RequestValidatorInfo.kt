package kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.validation.request

import kotlinx.serialization.json.JsonObject
import kz.mybrain.ofdcodec.domain.model.CommandType
import kz.mybrain.ofdcodec.domain.model.ValidationError
import kz.mybrain.ofdcodec.domain.port.Validator
import kz.mybrain.ofdcodec.domain.validation.ValidationUtils
import kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.validation.request.service.ServiceRequestValidator

/**
 * Валидатор запроса для COMMAND_INFO.
 */
internal class RequestValidatorInfo : Validator {
    /**
     * Проверяет JSON и собирает все ошибки по полям без остановки на первой.
     * В COMMAND_INFO обязательна служебная часть service.
     */
    override fun validate(commandType: CommandType, json: JsonObject): List<ValidationError> {
        val errors = mutableListOf<ValidationError>()

        // В COMMAND_INFO обязателен объект service.
        val service = json["service"] as? JsonObject
        if (service == null) {
            errors.add(ValidationUtils.missingField("$.payload.service"))
            return errors
        }

        // Полная проверка service (offlinePeriod, securityStats, regInfo, getRegInfo).
        val serviceValidator = ServiceRequestValidator()
        errors.addAll(serviceValidator.validate(service, "$.payload.service"))

        return errors
    }
}
