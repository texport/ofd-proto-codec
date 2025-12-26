package kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.codec.enums

import kz.kazakhtelecom.proto.v203.Ticket
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * Чтение ItemTypeEnum для TicketRequest.Item из JSON.
 */
class TicketItemTypeBuilder {
    /**
     * Читает ItemTypeEnum по ключу и возвращает его значение.
     */
    fun readRequired(json: JsonObject, key: String): Ticket.TicketRequest.Item.ItemTypeEnum {
        val value = readString(json, key)
        return Ticket.TicketRequest.Item.ItemTypeEnum.valueOf(value ?: throw IllegalArgumentException("Missing $key"))
    }

    /**
     * Читает строку, если поле присутствует.
     */
    private fun readString(json: JsonObject, key: String): String? {
        val element = json[key] as? JsonPrimitive ?: return null
        return if (element.isString) element.content else null
    }
}
