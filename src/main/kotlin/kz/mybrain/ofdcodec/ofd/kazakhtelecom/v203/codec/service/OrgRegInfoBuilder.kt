package kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.codec.service

import kz.kazakhtelecom.proto.v203.Reginfo
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive

/**
 * Сборщик proto OrgRegInfo из JSON-структуры.
 */
class OrgRegInfoBuilder {
    /**
     * Строит OrgRegInfo из JSON-объекта.
     */
    fun build(json: JsonObject): Reginfo.OrgRegInfo {
        val builder = Reginfo.OrgRegInfo.newBuilder()
            .setTitle(readStringRequired(json, "title"))
            .setAddress(readStringRequired(json, "address"))
            .setInn(readStringRequired(json, "inn"))
            .setAddressKz(readStringRequired(json, "addressKz"))
        
        val okved = json["okved"]?.jsonPrimitive?.content
        if (okved != null) {
            builder.setOkved(okved)
        }
        return builder.build()
    }

    /**
     * Читает обязательную строку или выбрасывает ошибку.
     */
    /**
     * Читает обязательную строку или выбрасывает ошибку.
     */
    private fun readStringRequired(json: JsonObject, key: String): String {
        val element = json[key] as? JsonPrimitive ?: throw IllegalArgumentException("Missing $key")
        require(element.isString) { "Invalid type for $key" }
        return element.content
    }
}
