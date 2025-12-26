package kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.validation.common.service

import kz.mybrain.ofdcodec.domain.model.ValidationError
import kz.mybrain.ofdcodec.domain.validation.ValidationUtils
import kotlinx.serialization.json.JsonObject

/**
 * Валидация OrgRegInfo для протокола Казахтелеком v203.
 */
class OrgRegInfoValidator {
    /**
     * Валидирует обязательные поля OrgRegInfo.
     * Проверяет JSON и собирает все ошибки по полям без остановки на первой.
     */
    fun validate(org: JsonObject, basePath: String): List<ValidationError> {
        val errors = mutableListOf<ValidationError>()

        // Название организации/ИП — обязательная строка.
        ValidationUtils.requireNonBlankString(org, "title", "$basePath.title", errors)
        // Юридический адрес — обязательная строка.
        ValidationUtils.requireNonBlankString(org, "address", "$basePath.address", errors)
        // Юридический адрес на гос. языке — обязательная строка.
        ValidationUtils.requireNonBlankString(org, "addressKz", "$basePath.addressKz", errors)
        // ИИН/БИН — обязательная строка.
        ValidationUtils.requireNonBlankString(org, "inn", "$basePath.inn", errors)
        // ОКЭД — обязательная строка.
        ValidationUtils.requireNonBlankString(org, "okved", "$basePath.okved", errors)

        return errors
    }
}
