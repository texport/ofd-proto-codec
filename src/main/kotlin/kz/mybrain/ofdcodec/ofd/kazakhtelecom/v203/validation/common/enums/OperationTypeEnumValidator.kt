package kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.validation.common.enums

import kz.kazakhtelecom.proto.v203.Common
import kz.mybrain.ofdcodec.domain.model.ValidationError
import kz.mybrain.ofdcodec.domain.validation.ValidationUtils
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * Валидация OperationTypeEnum для протокола Казахтелеком v203.
 */
class OperationTypeEnumValidator {
    /**
     * Валидирует enum по ключу в контейнере.
     * Проверяет JSON и собирает все ошибки по полям без остановки на первой.
     */
    fun validate(container: JsonObject, key: String, path: String): List<ValidationError> {
        val errors = mutableListOf<ValidationError>()
        val element = container[key] as? JsonPrimitive
        if (element == null) {
            errors.add(ValidationUtils.missingField(path))
            return errors
        }
        if (!element.isString) {
            errors.add(ValidationUtils.invalidType(path))
            return errors
        }
        val value = element.content
        val allowed = Common.OperationTypeEnum.values().map { it.name }.toSet()
        if (value !in allowed) {
            errors.add(ValidationUtils.invalidValue(path))
        }
        return errors
    }
}
