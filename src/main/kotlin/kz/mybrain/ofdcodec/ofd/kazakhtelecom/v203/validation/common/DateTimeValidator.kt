package kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.validation.common

import kotlinx.serialization.json.JsonObject
import kz.mybrain.ofdcodec.domain.model.ValidationError
import kz.mybrain.ofdcodec.domain.validation.ValidationUtils

/**
 * Валидация DateTime/Date/Time для протокола Казахтелеком v203.
 */
internal class DateTimeValidator {
    /**
     * Валидирует DateTime по ключу в контейнере.
     * Проверяет JSON и собирает все ошибки по полям без остановки на первой.
     */
    fun validate(container: JsonObject, key: String, path: String): List<ValidationError> {
        val errors = mutableListOf<ValidationError>()
        val dt = container[key] as? JsonObject
        if (dt == null) {
            errors.add(ValidationUtils.missingField(path))
            return errors
        }

        // В DateTime обязателен блок date.
        val date = dt["date"] as? JsonObject
        // В DateTime обязателен блок time.
        val time = dt["time"] as? JsonObject
        if (date == null) {
            errors.add(ValidationUtils.missingField("$path.date"))
        } else {
            validateDate(date, "$path.date", errors)
        }
        if (time == null) {
            errors.add(ValidationUtils.missingField("$path.time"))
        } else {
            validateTime(time, "$path.time", errors)
        }

        return errors
    }

    /**
     * Валидирует структуру Date.
     * Проверяет JSON и собирает все ошибки по полям без остановки на первой.
     */
    private fun validateDate(date: JsonObject, path: String, errors: MutableList<ValidationError>) {
        // Год: 4 цифры, диапазон 1000..9999.
        ValidationUtils.requireIntInRange(date, "year", 1000, 9999, "$path.year", errors)
        // Месяц: 1..12.
        ValidationUtils.requireIntInRange(date, "month", 1, 12, "$path.month", errors)
        // День: 1..31.
        ValidationUtils.requireIntInRange(date, "day", 1, 31, "$path.day", errors)
    }

    /**
     * Валидирует структуру Time.
     * Проверяет JSON и собирает все ошибки по полям без остановки на первой.
     */
    private fun validateTime(time: JsonObject, path: String, errors: MutableList<ValidationError>) {
        // Часы: 0..23.
        ValidationUtils.requireIntInRange(time, "hour", 0, 23, "$path.hour", errors)
        // Минуты: 0..59.
        ValidationUtils.requireIntInRange(time, "minute", 0, 59, "$path.minute", errors)
        val second = time["second"]
        if (second != null) {
            // Секунды опциональны, но если переданы — 0..59.
            ValidationUtils.requireIntInRange(time, "second", 0, 59, "$path.second", errors)
        }
    }
}
