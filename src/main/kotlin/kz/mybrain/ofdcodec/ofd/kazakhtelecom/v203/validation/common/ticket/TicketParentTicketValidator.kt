package kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.validation.common.ticket

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kz.mybrain.ofdcodec.domain.model.ValidationError
import kz.mybrain.ofdcodec.domain.validation.ValidationUtils
import kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.validation.common.DateTimeValidator
import kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.validation.common.MoneyValidator

/**
 * Валидация ParentTicket для TicketRequest.
 *
 * Используется для операций возврата. Все поля обязательны,
 * если parentTicket присутствует в JSON.
 */
class TicketParentTicketValidator {
    private val dateTimeValidator = DateTimeValidator()
    private val moneyValidator = MoneyValidator()

    /**
     * Валидирует ParentTicket по ключу в контейнере.
     * Проверяет JSON и собирает все ошибки по полям без остановки на первой.
     */
    fun validate(container: JsonObject, key: String, path: String): List<ValidationError> {
        val errors = mutableListOf<ValidationError>()
        val parent = container[key] as? JsonObject
        if (parent == null) {
            errors.add(ValidationUtils.missingField(path))
            return errors
        }
        ValidationUtils.requireNonBlankString(parent, "parentTicketNumber", "$path.parentTicketNumber", errors)
        errors.addAll(dateTimeValidator.validate(parent, "parentTicketDateTime", "$path.parentTicketDateTime"))
        ValidationUtils.requireNonBlankString(parent, "kgdKkmId", "$path.kgdKkmId", errors)
        errors.addAll(moneyValidator.validate(parent, "parentTicketTotal", "$path.parentTicketTotal"))
        val offline = parent["parentTicketIsOffline"]
        if (offline == null) {
            errors.add(ValidationUtils.missingField("$path.parentTicketIsOffline"))
        } else if (offline !is JsonPrimitive || offline.booleanOrNull == null) {
            errors.add(ValidationUtils.invalidType("$path.parentTicketIsOffline"))
        }
        return errors
    }
}
