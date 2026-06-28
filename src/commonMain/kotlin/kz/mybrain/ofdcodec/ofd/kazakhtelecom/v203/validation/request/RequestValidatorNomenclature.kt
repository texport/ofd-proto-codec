package kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.validation.request

import kotlinx.serialization.json.JsonObject
import kz.mybrain.ofdcodec.domain.model.CommandType
import kz.mybrain.ofdcodec.domain.model.ValidationError
import kz.mybrain.ofdcodec.domain.port.Validator
import kz.mybrain.ofdcodec.domain.validation.ValidationUtils
import kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.validation.request.service.ServiceRequestValidator

/**
 * Валидатор запроса для COMMAND_NOMENCLATURE.
 */
internal class RequestValidatorNomenclature : Validator {
    private val serviceValidator = ServiceRequestValidator()

    /**
     * Проверяет JSON и собирает все ошибки по полям без остановки на первой.
     */
    override fun validate(commandType: CommandType, json: JsonObject): List<ValidationError> {
        val errors = mutableListOf<ValidationError>()

        // В COMMAND_NOMENCLATURE сервисная часть обязательна для бизнес-валидаций.
        val service = json["service"] as? JsonObject
        if (service == null) {
            errors.add(ValidationUtils.missingField("$.payload.service"))
            return errors
        }
        // Полная проверка service (offlinePeriod, securityStats, regInfo, getRegInfo).
        errors.addAll(serviceValidator.validate(service, "$.payload.service"))

        // Блок nomenclature обязателен для команды номенклатуры.
        val nomenclature = json["nomenclature"] as? JsonObject
        if (nomenclature == null) {
            errors.add(ValidationUtils.missingField("$.payload.nomenclature"))
            return errors
        }

        val hasCurrentVersion = nomenclature["currentVersion"] != null
        val hasBarcode = nomenclature["barcode"] != null
        if (!hasCurrentVersion && !hasBarcode) {
            // В запросе должен быть currentVersion или barcode.
            errors.add(ValidationUtils.invalidValue("$.payload.nomenclature"))
            return errors
        }

        if (hasCurrentVersion) {
            // currentVersion опционален, но если есть — uint32 (>= 0).
            ValidationUtils.requireIntInRange(
                nomenclature,
                "currentVersion",
                0,
                Int.MAX_VALUE,
                "$.payload.nomenclature.currentVersion",
                errors
            )
        }

        // barcode опционален, но если есть — строка.
        ValidationUtils.optionalNonBlankString(nomenclature, "barcode", "$.payload.nomenclature.barcode", errors)

        return errors
    }
}
