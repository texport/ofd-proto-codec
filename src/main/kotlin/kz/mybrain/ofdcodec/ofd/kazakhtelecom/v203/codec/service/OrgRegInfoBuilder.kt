package kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.codec.service

import kz.kazakhtelecom.proto.v203.Reginfo
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * Сборщик proto OrgRegInfo из JSON-структуры.
 */
class OrgRegInfoBuilder {
    /**
     * Строит OrgRegInfo из JSON-объекта.
     */
    fun build(json: JsonObject): Reginfo.OrgRegInfo {
        return Reginfo.OrgRegInfo.newBuilder()
            .setTitle(readStringRequired(json, "title"))
            .setAddress(readStringRequired(json, "address"))
            .setInn(readStringRequired(json, "inn"))
            .setOkved(readStringRequired(json, "okved"))
            .setAddressKz(readStringRequired(json, "addressKz"))
            .build()
    }

    /**
     * Читает обязательную строку или выбрасывает ошибку.
     */
    /**
     * Читает обязательную строку или выбрасывает ошибку.
     */
    private fun readStringRequired(json: JsonObject, key: String): String {
        val element = json[key] as? JsonPrimitive ?: throw IllegalArgumentException("Missing $key")
        if (!element.isString) throw IllegalArgumentException("Invalid type for $key")
        return element.content
    }
}
