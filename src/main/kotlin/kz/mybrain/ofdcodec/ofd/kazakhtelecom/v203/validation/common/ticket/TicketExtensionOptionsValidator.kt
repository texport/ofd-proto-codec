package kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.validation.common.ticket

import kz.mybrain.ofdcodec.domain.model.ValidationError
import kz.mybrain.ofdcodec.domain.validation.ValidationUtils
import kotlinx.serialization.json.JsonObject

/**
 * Валидация ExtensionOptions для TicketRequest.
 *
 * Все поля опциональны и проверяются только на корректный тип/непустую строку.
 */
class TicketExtensionOptionsValidator {
    /**
     * Валидирует ExtensionOptions по ключу в контейнере.
     * Проверяет JSON и собирает все ошибки по полям без остановки на первой.
     */
    fun validate(container: JsonObject, key: String, path: String): List<ValidationError> {
        val errors = mutableListOf<ValidationError>()
        val options = container[key] as? JsonObject
        if (options == null) {
            errors.add(ValidationUtils.missingField(path))
            return errors
        }
        ValidationUtils.optionalNonBlankString(options, "customerEmail", "$path.customerEmail", errors)
        ValidationUtils.optionalNonBlankString(options, "customerPhone", "$path.customerPhone", errors)
        ValidationUtils.optionalNonBlankString(options, "customerIinOrBin", "$path.customerIinOrBin", errors)
        return errors
    }
}
