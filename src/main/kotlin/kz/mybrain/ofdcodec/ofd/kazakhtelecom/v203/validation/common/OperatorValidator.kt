package kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.validation.common

import kz.mybrain.ofdcodec.domain.model.ValidationError
import kz.mybrain.ofdcodec.domain.validation.ValidationUtils
import kotlinx.serialization.json.JsonObject

/**
 * Валидация структуры Operator для протокола Казахтелеком v203.
 *
 * code обязателен, name опционален (если указан — не пустой).
 */
class OperatorValidator {
    /**
     * Валидирует Operator по ключу в контейнере.
     * Проверяет JSON и собирает все ошибки по полям без остановки на первой.
     */
    fun validate(container: JsonObject, key: String, path: String): List<ValidationError> {
        val errors = mutableListOf<ValidationError>()
        val operator = container[key] as? JsonObject
        if (operator == null) {
            errors.add(ValidationUtils.missingField(path))
            return errors
        }

        // code: обязательный код оператора-кассира, диапазон uint32.
        ValidationUtils.requireIntInRange(operator, "code", 0, Int.MAX_VALUE, "$path.code", errors)
        // name: опциональное имя оператора; если указано, должно быть строкой.
        ValidationUtils.optionalNonBlankString(operator, "name", "$path.name", errors)

        return errors
    }
}
