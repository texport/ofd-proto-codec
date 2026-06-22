package kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.validation.common.enums

import kotlinx.serialization.json.JsonObject
import kz.kazakhtelecom.proto.v203.Ticket
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
        private val ALLOWED = Ticket.TicketRequest.Item.ItemTypeEnum.entries.map { it.name }.toSet()
    }
}
