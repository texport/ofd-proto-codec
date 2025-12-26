package kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.validation.common.ticket

import kz.mybrain.ofdcodec.domain.model.ValidationError
import kz.mybrain.ofdcodec.domain.validation.ValidationUtils
import kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.validation.common.MoneyValidator
import kotlinx.serialization.json.JsonObject

/**
 * Валидация Amounts для TicketRequest.
 *
 * Обязательное поле: total.
 * taken/change опциональны и требуются только при наличной оплате
 * (это проверяется на уровне RequestValidatorTicket).
 */
class TicketAmountsValidator {
    private val moneyValidator = MoneyValidator()
    private val modifierValidator = TicketModifierValidator()

    /**
     * Валидирует Amounts по ключу в контейнере.
     * Проверяет JSON и собирает все ошибки по полям без остановки на первой.
     */
    fun validate(container: JsonObject, key: String, path: String): List<ValidationError> {
        val errors = mutableListOf<ValidationError>()
        val amounts = container[key] as? JsonObject
        if (amounts == null) {
            errors.add(ValidationUtils.missingField(path))
            return errors
        }
        errors.addAll(moneyValidator.validate(amounts, "total", "$path.total"))
        if (amounts["taken"] != null) {
            errors.addAll(moneyValidator.validate(amounts, "taken", "$path.taken"))
        }
        if (amounts["change"] != null) {
            errors.addAll(moneyValidator.validate(amounts, "change", "$path.change"))
        }
        if (amounts["markup"] != null) {
            errors.addAll(modifierValidator.validate(amounts, "markup", "$path.markup"))
        }
        if (amounts["discount"] != null) {
            errors.addAll(modifierValidator.validate(amounts, "discount", "$path.discount"))
        }
        return errors
    }
}
