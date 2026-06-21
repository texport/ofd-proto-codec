package kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.validation.common.zxreport

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kz.mybrain.ofdcodec.domain.model.ValidationError
import kz.mybrain.ofdcodec.domain.validation.ValidationUtils
import kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.validation.common.MoneyValidator
import kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.validation.common.enums.OperationTypeEnumValidator

/**
 * Валидация TicketOperation внутри ZXReport.
 *
 * payments считается опциональным: если поле отсутствует в ответе, ошибки не формируются.
 */
class ZXReportTicketOperationValidator {
    private val operationTypeValidator = OperationTypeEnumValidator()
    private val moneyValidator = MoneyValidator()
    private val paymentValidator = ZXReportTicketPaymentValidator()

    /**
     * Валидирует список операций по чекам по ключу в контейнере.
     */
    fun validateList(container: JsonObject, key: String, path: String): List<ValidationError> {
        val errors = mutableListOf<ValidationError>()
        val ticketOps = container[key] ?: return errors
        val array = ticketOps as? JsonArray
        if (array == null) {
            errors.add(ValidationUtils.invalidType(path))
            return errors
        }
        array.forEachIndexed { index, op ->
            val opPath = "$path[$index]"
            val opObj = op as? JsonObject
            if (opObj == null) {
                errors.add(ValidationUtils.invalidType(opPath))
            } else {
                errors.addAll(validate(opObj, opPath))
            }
        }
        return errors
    }

    /**
     * Валидирует одну операцию по чеку.
     */
    fun validate(operation: JsonObject, path: String): List<ValidationError> {
        val errors = mutableListOf<ValidationError>()
        errors.addAll(operationTypeValidator.validate(operation, "operation", "$path.operation"))
        ValidationUtils.requireIntInRange(
            operation,
            "ticketsTotalCount",
            0,
            Int.MAX_VALUE,
            "$path.ticketsTotalCount",
            errors
        )
        ValidationUtils.requireIntInRange(operation, "ticketsCount", 0, Int.MAX_VALUE, "$path.ticketsCount", errors)
        errors.addAll(moneyValidator.validate(operation, "ticketsSum", "$path.ticketsSum"))
        val paymentsElement = operation["payments"]
        if (paymentsElement != null) {
            val payments = paymentsElement as? JsonArray
            if (payments == null) {
                errors.add(ValidationUtils.invalidType("$path.payments"))
            } else {
                errors.addAll(paymentValidator.validateList(operation, "payments", "$path.payments"))
            }
        }
        ValidationUtils.requireIntInRange(operation, "offlineCount", 0, Int.MAX_VALUE, "$path.offlineCount", errors)
        errors.addAll(moneyValidator.validate(operation, "discountSum", "$path.discountSum"))
        errors.addAll(moneyValidator.validate(operation, "markupSum", "$path.markupSum"))
        errors.addAll(moneyValidator.validate(operation, "changeSum", "$path.changeSum"))
        return errors
    }
}
