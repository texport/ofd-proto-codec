package kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.codec.service

import kz.kazakhtelecom.proto.v203.Reginfo
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * Сборщик proto KkmRegInfo из JSON-структуры.
 */
class KkmRegInfoBuilder {
    /**
     * Строит KkmRegInfo из JSON-объекта.
     */
    fun build(json: JsonObject): Reginfo.KkmRegInfo {
        val builder = Reginfo.KkmRegInfo.newBuilder()

        readString(json, "pointOfPaymentNumber")?.let { builder.setPointOfPaymentNumber(it) }
        readString(json, "terminalNumber")?.let { builder.setTerminalNumber(it) }
        builder.setFnsKkmId(readStringRequired(json, "fnsKkmId"))
        builder.setSerialNumber(readStringRequired(json, "serialNumber"))
        builder.setKkmId(readStringRequired(json, "kkmId"))

        return builder.build()
    }

    /**
     * Читает строку, если поле присутствует.
     */
    /**
     * Читает строку, если поле присутствует.
     */
    private fun readString(json: JsonObject, key: String): String? {
        val element = json[key] as? JsonPrimitive ?: return null
        return if (element.isString) element.content else null
    }

    /**
     * Читает обязательную строку или выбрасывает ошибку.
     */
    /**
     * Читает обязательную строку или выбрасывает ошибку.
     */
    private fun readStringRequired(json: JsonObject, key: String): String {
        val value = readString(json, key)
        return value ?: throw IllegalArgumentException("Missing $key")
    }
}
