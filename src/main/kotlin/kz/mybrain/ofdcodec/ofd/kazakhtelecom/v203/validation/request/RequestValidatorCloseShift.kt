package kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.validation.request

import kz.mybrain.ofdcodec.domain.model.CommandType
import kz.mybrain.ofdcodec.domain.model.ValidationError
import kz.mybrain.ofdcodec.domain.port.Validator
import kz.mybrain.ofdcodec.domain.validation.ValidationUtils
import kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.validation.common.DateTimeValidator
import kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.validation.common.OperatorValidator
import kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.validation.common.zxreport.ZXReportValidator
import kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.validation.request.service.ServiceRequestValidator
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull

/**
 * Валидатор запроса для COMMAND_CLOSE_SHIFT.
 */
class RequestValidatorCloseShift : Validator {
    private val serviceValidator = ServiceRequestValidator()
    private val dateTimeValidator = DateTimeValidator()
    private val zxReportValidator = ZXReportValidator()
    private val operatorValidator = OperatorValidator()

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

        val closeShift = json["closeShift"] as? JsonObject
        if (closeShift == null) {
            errors.add(ValidationUtils.missingField("$.payload.closeShift"))
            return errors
        }

        errors.addAll(dateTimeValidator.validate(closeShift, "closeTime", "$.payload.closeShift.closeTime"))
        errors.addAll(zxReportValidator.validate(closeShift, "zReport", "$.payload.closeShift.zReport", "REPORT_Z"))
        errors.addAll(operatorValidator.validate(closeShift, "operator", "$.payload.closeShift.operator"))

        val isOffline = closeShift["isOffline"]
        if (isOffline != null && (isOffline !is JsonPrimitive || isOffline.booleanOrNull == null)) {
            errors.add(ValidationUtils.invalidType("$.payload.closeShift.isOffline"))
        }

        if (closeShift["frShiftNumber"] != null) {
            ValidationUtils.requireIntInRange(
                closeShift,
                "frShiftNumber",
                0,
                Int.MAX_VALUE,
                "$.payload.closeShift.frShiftNumber",
                errors
            )
        }
        val withdrawMoney = closeShift["withdrawMoney"]
        if (withdrawMoney != null && (withdrawMoney !is JsonPrimitive || withdrawMoney.booleanOrNull == null)) {
            errors.add(ValidationUtils.invalidType("$.payload.closeShift.withdrawMoney"))
        }
        if (closeShift["printedDocumentNumber"] != null) {
            ValidationUtils.requireLongInRange(
                closeShift,
                "printedDocumentNumber",
                0,
                Long.MAX_VALUE,
                "$.payload.closeShift.printedDocumentNumber",
                errors
            )
        }

        return errors
    }
}
