package kz.mybrain.ofdcodec.ofd.bfd.v204.validation.common.enums

import kotlinx.serialization.json.JsonObject
import kz.bfd.proto.v204.PaymentTypeEnum
import kz.mybrain.ofdcodec.domain.model.ValidationError
import kz.mybrain.ofdcodec.domain.validation.ValidationUtils

/**
 * Валидация PaymentTypeEnum для текущего provider module v203.
 */
internal class PaymentTypeEnumValidator {
    /**
     * Валидирует enum по ключу в контейнере.
     * Проверяет JSON и собирает все ошибки по полям без остановки на первой.
     */
    fun validate(container: JsonObject, key: String, path: String): List<ValidationError> {
        return ValidationUtils.validateEnum(container, key, path, ALLOWED)
    }

    companion object {
        private val ALLOWED = PaymentTypeEnum.entries.map { it.name }.toSet()
    }
}
