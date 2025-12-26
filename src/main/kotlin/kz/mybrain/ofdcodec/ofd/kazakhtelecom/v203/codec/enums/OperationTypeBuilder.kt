package kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.codec.enums

import kz.kazakhtelecom.proto.v203.Common
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * Чтение OperationTypeEnum из JSON.
 */
class OperationTypeBuilder {
    /**
     * Читает OperationTypeEnum по ключу и возвращает его значение.
     */
    fun readRequired(json: JsonObject, key: String): Common.OperationTypeEnum {
        val value = readString(json, key)
        return Common.OperationTypeEnum.valueOf(value ?: throw IllegalArgumentException("Missing $key"))
    }

    /**
     * Читает строку, если поле присутствует.
     */
    private fun readString(json: JsonObject, key: String): String? {
        val element = json[key] as? JsonPrimitive ?: return null
        return if (element.isString) element.content else null
    }
}
