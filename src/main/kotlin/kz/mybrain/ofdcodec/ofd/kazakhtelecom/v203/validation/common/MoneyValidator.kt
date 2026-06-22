package kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.validation.common

import kotlinx.serialization.json.JsonObject
import kz.mybrain.ofdcodec.domain.model.ValidationError
import kz.mybrain.ofdcodec.domain.validation.ValidationUtils

/**
 * Валидация структуры Money для протокола Казахтелеком v203.
 *
 * Оба поля обязательны: bills (uint64) и coins (uint32).
 */
internal class MoneyValidator {
    /**
     * Валидирует Money по ключу в контейнере.
     * Проверяет JSON и собирает все ошибки по полям без остановки на первой.
     */
    fun validate(container: JsonObject, key: String, path: String): List<ValidationError> {
        val errors = mutableListOf<ValidationError>()
        val money = container[key] as? JsonObject
        if (money == null) {
            errors.add(ValidationUtils.missingField(path))
            return errors
        }

        // bills: обязательная сумма основных денежных единиц, диапазон uint64.
        ValidationUtils.requireLongInRange(money, "bills", 0, Long.MAX_VALUE, "$path.bills", errors)
        // coins: обязательная сумма разменных единиц, диапазон uint32.
        ValidationUtils.requireIntInRange(money, "coins", 0, Int.MAX_VALUE, "$path.coins", errors)

        return errors
    }
}
