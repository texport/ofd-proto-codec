package kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.codec.enums

import kz.kazakhtelecom.proto.v203.*

import kotlinx.serialization.json.JsonObject

import kz.mybrain.ofdcodec.infrastructure.json.readString

/**
 * Чтение OperationTypeEnum из JSON.
 */
internal class OperationTypeBuilder {
    /**
     * Читает OperationTypeEnum по ключу и возвращает его значение.
     */
    fun readRequired(json: JsonObject, key: String): OperationTypeEnum {
        val value = json.readString(key)
        return OperationTypeEnum.valueOf(
            value ?: throw IllegalArgumentException("Missing $key / Отсутствует $key / $key өрісі жетіспейді")
        )
    }
}
