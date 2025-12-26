package kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.validation.common.zxreport

import kz.mybrain.ofdcodec.domain.model.ValidationError
import kz.mybrain.ofdcodec.domain.validation.ValidationUtils
import kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.validation.common.MoneyValidator
import kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.validation.common.enums.PaymentTypeEnumValidator
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject

/**
 * Валидация Payment внутри TicketOperation.
 */
class ZXReportTicketPaymentValidator {
    private val paymentTypeValidator = PaymentTypeEnumValidator()
    private val moneyValidator = MoneyValidator()

    /**
     * Валидирует список оплат по ключу в контейнере.
     */
    fun validateList(container: JsonObject, key: String, path: String): List<ValidationError> {
        val errors = mutableListOf<ValidationError>()
        val payments = container[key] ?: return errors
        val array = payments as? JsonArray
        if (array == null) {
            errors.add(ValidationUtils.invalidType(path))
            return errors
        }
        array.forEachIndexed { index, payment ->
            val payPath = "$path[$index]"
            val payObj = payment as? JsonObject
            if (payObj == null) {
                errors.add(ValidationUtils.invalidType(payPath))
            } else {
                errors.addAll(validate(payObj, payPath))
            }
        }
        return errors
    }

    /**
     * Валидирует одну оплату.
     */
    fun validate(payment: JsonObject, path: String): List<ValidationError> {
        val errors = mutableListOf<ValidationError>()
        errors.addAll(paymentTypeValidator.validate(payment, "payment", "$path.payment"))
        errors.addAll(moneyValidator.validate(payment, "sum", "$path.sum"))
        ValidationUtils.requireIntInRange(payment, "count", 0, Int.MAX_VALUE, "$path.count", errors)
        return errors
    }
}
