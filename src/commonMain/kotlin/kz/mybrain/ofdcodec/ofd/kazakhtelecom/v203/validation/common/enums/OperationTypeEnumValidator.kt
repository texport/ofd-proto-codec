package kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.validation.common.enums

import kotlinx.serialization.json.JsonObject
import kz.kazakhtelecom.proto.v203.*
import kz.mybrain.ofdcodec.domain.model.ValidationError
import kz.mybrain.ofdcodec.domain.validation.ValidationUtils

/**
 * Валидация OperationTypeEnum для протокола Казахтелеком v203.
 */
internal class OperationTypeEnumValidator {
    /**
     * Валидирует enum по ключу в контейнере.
     * Проверяет JSON и собирает все ошибки по полям без остановки на первой.
     */
    fun validate(container: JsonObject, key: String, path: String): List<ValidationError> {
        return ValidationUtils.validateEnum(container, key, path, ALLOWED)
    }

    companion object {
        private val ALLOWED = OperationTypeEnum.entries.map { it.name }.toSet()
    }
}
