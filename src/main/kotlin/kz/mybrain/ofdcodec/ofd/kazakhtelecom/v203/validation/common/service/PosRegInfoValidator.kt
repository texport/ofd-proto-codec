package kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.validation.common.service

import kz.mybrain.ofdcodec.domain.model.ValidationError
import kz.mybrain.ofdcodec.domain.validation.ValidationUtils
import kotlinx.serialization.json.JsonObject

/**
 * Валидация PosRegInfo для ответов ОФД Казахтелеком v203.
 */
class PosRegInfoValidator {
    /**
     * Валидирует поля торговой точки в ответе сервера.
     * Проверяет JSON и собирает все ошибки по полям без остановки на первой.
     */
    fun validate(pos: JsonObject, basePath: String): List<ValidationError> {
        val errors = mutableListOf<ValidationError>()

        // Название торговой точки — обязательная строка.
        ValidationUtils.requireNonBlankString(pos, "title", "$basePath.title", errors)
        // Адрес торговой точки — обязательная строка.
        ValidationUtils.requireNonBlankString(pos, "address", "$basePath.address", errors)
        // Адрес на гос. языке — обязательная строка.
        ValidationUtils.requireNonBlankString(pos, "addressKz", "$basePath.addressKz", errors)
        // Широта — обязательное число >= 0.
        ValidationUtils.requireIntInRange(pos, "latitude", 0, Int.MAX_VALUE, "$basePath.latitude", errors)
        // Долгота — обязательное число >= 0.
        ValidationUtils.requireIntInRange(pos, "longitude", 0, Int.MAX_VALUE, "$basePath.longitude", errors)

        return errors
    }
}
