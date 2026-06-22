package kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.validation.response

import kotlinx.serialization.json.JsonObject
import kz.mybrain.ofdcodec.domain.model.ValidationError
import kz.mybrain.ofdcodec.domain.validation.ValidationUtils

/**
 * Валидация TicketAdInfo для ответов ОФД Казахтелеком v203.
 */
internal class TicketAdInfoValidator {
    /**
     * Валидирует тип и версию рекламного блока.
     * Проверяет JSON и собирает все ошибки по полям без остановки на первой.
     */
    fun validate(info: JsonObject, basePath: String): List<ValidationError> {
        val errors = mutableListOf<ValidationError>()
        // Тип рекламного блока — обязательная строка (имя enum).
        ValidationUtils.requireNonBlankString(info, "type", "$basePath.type", errors)
        // Версия рекламного блока — обязательное uint64 значение.
        ValidationUtils.requireLongInRange(info, "version", 0, Long.MAX_VALUE, "$basePath.version", errors)
        return errors
    }
}
