package kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.codec.common

import kz.kazakhtelecom.proto.v203.Common
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull

/**
 * Сборщик Operator из JSON-структуры.
 */
class OperatorBuilder {
    /**
     * Строит Operator из JSON-объекта.
     */
    fun build(operatorJson: JsonObject): Common.Operator {
        val builder = Common.Operator.newBuilder()
        builder.setCode(readIntRequired(operatorJson, "code"))
        readString(operatorJson, "name")?.let { builder.setName(it) }
        return builder.build()
    }

    /**
     * Читает строку, если поле присутствует.
     */
    private fun readString(json: JsonObject, key: String): String? {
        val element = json[key] as? JsonPrimitive ?: return null
        return if (element.isString) element.content else null
    }

    /**
     * Читает int, если поле присутствует.
     */
    private fun readInt(json: JsonObject, key: String): Int? {
        val element = json[key] as? JsonPrimitive ?: return null
        return element.intOrNull
    }

    /**
     * Читает обязательный int или выбрасывает ошибку.
     */
    private fun readIntRequired(json: JsonObject, key: String): Int {
        val value = readInt(json, key)
        return value ?: throw IllegalArgumentException("Missing $key")
    }
}
