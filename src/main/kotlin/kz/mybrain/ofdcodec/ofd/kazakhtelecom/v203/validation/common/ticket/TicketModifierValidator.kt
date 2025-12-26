package kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.validation.common.ticket

import kz.mybrain.ofdcodec.domain.model.ValidationError
import kz.mybrain.ofdcodec.domain.validation.ValidationUtils
import kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.validation.common.MoneyValidator
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull

/**
 * Валидация Modifier для TicketRequest.
 *
 * Используется для скидок/наценок и их сторно.
 * Обязательны name и sum, taxes опциональны.
 */
class TicketModifierValidator {
    private val moneyValidator = MoneyValidator()
    private val taxValidator = TicketTaxValidator()

    /**
     * Валидирует Modifier по ключу в контейнере.
     * Проверяет JSON и собирает все ошибки по полям без остановки на первой.
     */
    fun validate(container: JsonObject, key: String, path: String): List<ValidationError> {
        val errors = mutableListOf<ValidationError>()
        val modifier = container[key] as? JsonObject
        if (modifier == null) {
            errors.add(ValidationUtils.missingField(path))
            return errors
        }
        ValidationUtils.requireNonBlankString(modifier, "name", "$path.name", errors)
        errors.addAll(moneyValidator.validate(modifier, "sum", "$path.sum"))

        val taxes = modifier["taxes"]
        if (taxes != null) {
            val array = taxes as? JsonArray
            if (array == null) {
                errors.add(ValidationUtils.invalidType("$path.taxes"))
            } else {
                val percents = array.mapNotNull { (it as? JsonObject)?.get("percent") as? JsonPrimitive }
                    .mapNotNull { primitive -> primitive.intOrNull }
                if (percents.size != percents.toSet().size) {
                    errors.add(ValidationUtils.invalidValue("$path.taxes"))
                }
                array.forEachIndexed { index, tax ->
                    val taxObj = tax as? JsonObject
                    if (taxObj == null) {
                        errors.add(ValidationUtils.invalidType("$path.taxes[$index]"))
                    } else {
                        errors.addAll(taxValidator.validateObject(taxObj, "$path.taxes[$index]"))
                    }
                }
            }
        }
        return errors
    }
}
