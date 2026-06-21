package kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.validation.request

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kz.mybrain.ofdcodec.domain.model.CommandType
import kz.mybrain.ofdcodec.domain.model.ValidationError
import kz.mybrain.ofdcodec.domain.port.Validator
import kz.mybrain.ofdcodec.domain.validation.ValidationUtils
import kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.validation.common.DateTimeValidator
import kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.validation.common.MoneyValidator
import kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.validation.common.OperatorValidator
import kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.validation.request.service.ServiceRequestValidator

/**
 * Валидатор запроса для COMMAND_MONEY_PLACEMENT.
 */
class RequestValidatorMoneyPlacement : Validator {
    private val dateTimeValidator = DateTimeValidator()
    private val moneyValidator = MoneyValidator()
    private val operatorValidator = OperatorValidator()
    private val serviceValidator = ServiceRequestValidator()

    /**
     * Проверяет JSON и собирает все ошибки по полям без остановки на первой.
     */
    override fun validate(commandType: CommandType, json: JsonObject): List<ValidationError> {
        val errors = mutableListOf<ValidationError>()

        // В COMMAND_MONEY_PLACEMENT сервисная часть обязательна для бизнес-валидаций.
        val service = json["service"] as? JsonObject
        if (service == null) {
            errors.add(ValidationUtils.missingField("$.payload.service"))
            return errors
        }
        // Полная проверка service (offlinePeriod, securityStats, regInfo, getRegInfo).
        errors.addAll(serviceValidator.validate(service, "$.payload.service"))

        // Блок moneyPlacement обязателен: без него невозможно сформировать команду.
        val moneyPlacement = json["moneyPlacement"] as? JsonObject
        if (moneyPlacement == null) {
            errors.add(ValidationUtils.missingField("$.payload.moneyPlacement"))
            return errors
        }

        // DateTime обязателен: без даты/времени операция недействительна.
        errors.addAll(dateTimeValidator.validate(moneyPlacement, "dateTime", "$.payload.moneyPlacement.dateTime"))

        // operation обязателен: указывает тип операции (внесение/снятие).
        val operationElement = moneyPlacement["operation"]
        if (operationElement == null) {
            errors.add(ValidationUtils.missingField("$.payload.moneyPlacement.operation"))
        } else if (operationElement !is JsonPrimitive || !operationElement.isString) {
            errors.add(ValidationUtils.invalidType("$.payload.moneyPlacement.operation"))
        } else {
            val allowed = setOf("MONEY_PLACEMENT_DEPOSIT", "MONEY_PLACEMENT_WITHDRAWAL")
            if (operationElement.content !in allowed) {
                errors.add(ValidationUtils.invalidValue("$.payload.moneyPlacement.operation"))
            }
        }

        // sum обязателен: проверяем структуру Money (bills + coins).
        errors.addAll(moneyValidator.validate(moneyPlacement, "sum", "$.payload.moneyPlacement.sum"))

        // isOffline опционален, но если есть — boolean.
        val isOffline = moneyPlacement["isOffline"]
        if (isOffline != null && (isOffline !is JsonPrimitive || isOffline.booleanOrNull == null)) {
            errors.add(ValidationUtils.invalidType("$.payload.moneyPlacement.isOffline"))
        }

        // frShiftNumber опционален, но если есть — uint32 (>= 0).
        val frShiftNumber = moneyPlacement["frShiftNumber"]
        if (frShiftNumber != null) {
            ValidationUtils.requireIntInRange(
                moneyPlacement,
                "frShiftNumber",
                0,
                Int.MAX_VALUE,
                "$.payload.moneyPlacement.frShiftNumber",
                errors
            )
        }

        // printedDocumentNumber опционален, но если есть — uint64 (>= 0).
        val printedDocumentNumber = moneyPlacement["printedDocumentNumber"]
        if (printedDocumentNumber != null) {
            ValidationUtils.requireLongInRange(
                moneyPlacement,
                "printedDocumentNumber",
                0,
                Long.MAX_VALUE,
                "$.payload.moneyPlacement.printedDocumentNumber",
                errors
            )
        }

        // operator обязателен для протокола 2.0.3: проверяем структуру Operator (code + name).
        errors.addAll(operatorValidator.validate(moneyPlacement, "operator", "$.payload.moneyPlacement.operator"))

        return errors
    }
}
