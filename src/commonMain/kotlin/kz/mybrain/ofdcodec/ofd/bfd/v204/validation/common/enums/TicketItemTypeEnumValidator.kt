package kz.mybrain.ofdcodec.ofd.bfd.v204.validation.common.enums

import kotlinx.serialization.json.JsonObject
import kz.bfd.proto.v204.TicketRequest
import kz.mybrain.ofdcodec.domain.model.ValidationError
import kz.mybrain.ofdcodec.domain.validation.ValidationUtils

/**
 * Валидация ItemTypeEnum для TicketRequest.Item.
 */
internal class TicketItemTypeEnumValidator {
    /**
     * Валидирует enum по ключу в контейнере.
     * Проверяет JSON и собирает все ошибки по полям без остановки на первой.
     */
    fun validate(container: JsonObject, key: String, path: String): List<ValidationError> {
        return ValidationUtils.validateEnum(container, key, path, ALLOWED)
    }

    companion object {
        private val ALLOWED = TicketRequest.Item.ItemTypeEnum.entries.map { it.name }.toSet()
    }
}
