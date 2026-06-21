package kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.validation.common.ticket

import kotlinx.serialization.json.JsonObject
import kz.mybrain.ofdcodec.domain.model.ValidationError
import kz.mybrain.ofdcodec.domain.validation.ValidationUtils
import kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.validation.common.MoneyValidator
import kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.validation.common.enums.PaymentTypeEnumValidator

/**
 * Валидация Payment для TicketRequest.
 *
 * Обязательные поля: type и sum.
 * cardPaymentFields и mobilePaymentFields опциональны.
 */
class TicketPaymentValidator {
    private val paymentTypeValidator = PaymentTypeEnumValidator()
    private val moneyValidator = MoneyValidator()

    /**
     * Валидирует Payment по ключу в контейнере.
     * Проверяет JSON и собирает все ошибки по полям без остановки на первой.
     */
    fun validate(container: JsonObject, key: String, path: String): List<ValidationError> {
        val errors = mutableListOf<ValidationError>()
        val payment = container[key] as? JsonObject
        if (payment == null) {
            errors.add(ValidationUtils.missingField(path))
            return errors
        }
        return validateObject(payment, path)
    }

    /**
     * Валидирует Payment объект без контейнера.
     */
    fun validateObject(payment: JsonObject, path: String): List<ValidationError> {
        val errors = mutableListOf<ValidationError>()
        errors.addAll(paymentTypeValidator.validate(payment, "type", "$path.type"))
        errors.addAll(moneyValidator.validate(payment, "sum", "$path.sum"))

        val card = payment["cardPaymentFields"] as? JsonObject
        if (card != null) {
            ValidationUtils.optionalNonBlankString(
                card,
                "posTerminalId",
                "$path.cardPaymentFields.posTerminalId",
                errors
            )
            ValidationUtils.optionalNonBlankString(card, "posCardType", "$path.cardPaymentFields.posCardType", errors)
            if (card["posAutorizationCode"] != null) {
                ValidationUtils.requireIntInRange(
                    card,
                    "posAutorizationCode",
                    0,
                    Int.MAX_VALUE,
                    "$path.cardPaymentFields.posAutorizationCode",
                    errors
                )
            }
            if (card["posRrn"] != null) {
                ValidationUtils.requireLongInRange(
                    card,
                    "posRrn",
                    0,
                    Long.MAX_VALUE,
                    "$path.cardPaymentFields.posRrn",
                    errors
                )
            }
            if (card["posReceiptNumber"] != null) {
                ValidationUtils.requireIntInRange(
                    card,
                    "posReceiptNumber",
                    0,
                    Int.MAX_VALUE,
                    "$path.cardPaymentFields.posReceiptNumber",
                    errors
                )
            }
        } else if (payment["cardPaymentFields"] != null) {
            errors.add(ValidationUtils.invalidType("$path.cardPaymentFields"))
        }

        val mobile = payment["mobilePaymentFields"] as? JsonObject
        if (mobile != null) {
            ValidationUtils.optionalNonBlankString(mobile, "qrType", "$path.mobilePaymentFields.qrType", errors)
            ValidationUtils.optionalNonBlankString(mobile, "qrId", "$path.mobilePaymentFields.qrId", errors)
        } else if (payment["mobilePaymentFields"] != null) {
            errors.add(ValidationUtils.invalidType("$path.mobilePaymentFields"))
        }

        return errors
    }
}
