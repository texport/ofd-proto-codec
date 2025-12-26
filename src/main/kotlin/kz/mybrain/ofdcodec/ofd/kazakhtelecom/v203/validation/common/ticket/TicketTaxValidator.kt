package kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.validation.common.ticket

import kz.mybrain.ofdcodec.domain.model.ValidationError
import kz.mybrain.ofdcodec.domain.validation.ValidationUtils
import kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.validation.common.MoneyValidator
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull

/**
 * Валидация Tax для TicketRequest.
 *
 * Обязательные поля: taxType, percent, sum, isInTotalSum.
 * Поле taxationType опционально.
 */
class TicketTaxValidator {
    private val moneyValidator = MoneyValidator()

    /**
     * Валидирует Tax по ключу в контейнере.
     * Проверяет JSON и собирает все ошибки по полям без остановки на первой.
     */
    fun validate(container: JsonObject, key: String, path: String): List<ValidationError> {
        val errors = mutableListOf<ValidationError>()
        val tax = container[key] as? JsonObject
        if (tax == null) {
            errors.add(ValidationUtils.missingField(path))
            return errors
        }
        return validateObject(tax, path)
    }

    /**
     * Валидирует Tax объект без контейнера.
     */
    fun validateObject(tax: JsonObject, path: String): List<ValidationError> {
        val errors = mutableListOf<ValidationError>()
        ValidationUtils.requireIntInRange(tax, "taxType", 0, Int.MAX_VALUE, "$path.taxType", errors)
        if (tax["taxationType"] != null) {
            ValidationUtils.requireIntInRange(tax, "taxationType", 0, Int.MAX_VALUE, "$path.taxationType", errors)
        }
        ValidationUtils.requireIntInRange(tax, "percent", 0, Int.MAX_VALUE, "$path.percent", errors)
        errors.addAll(moneyValidator.validate(tax, "sum", "$path.sum"))
        val isInTotal = tax["isInTotalSum"]
        if (isInTotal == null) {
            errors.add(ValidationUtils.missingField("$path.isInTotalSum"))
        } else if (isInTotal !is JsonPrimitive || isInTotal.booleanOrNull == null) {
            errors.add(ValidationUtils.invalidType("$path.isInTotalSum"))
        }
        return errors
    }
}
