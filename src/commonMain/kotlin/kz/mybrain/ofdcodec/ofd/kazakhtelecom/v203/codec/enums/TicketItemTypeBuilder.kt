package kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.codec.enums

import kz.kazakhtelecom.proto.v203.*

import kotlinx.serialization.json.JsonObject

import kz.mybrain.ofdcodec.infrastructure.json.readString

/**
 * Чтение ItemTypeEnum для TicketRequest.Item из JSON.
 */
internal class TicketItemTypeBuilder {
    /**
     * Читает ItemTypeEnum по ключу и возвращает его значение.
     */
    fun readRequired(json: JsonObject, key: String): TicketRequest.Item.ItemTypeEnum {
        val value = json.readString(key)
        return TicketRequest.Item.ItemTypeEnum.valueOf(
            value ?: throw IllegalArgumentException("Missing $key / Отсутствует $key / $key өрісі жетіспейді")
        )
    }
}
