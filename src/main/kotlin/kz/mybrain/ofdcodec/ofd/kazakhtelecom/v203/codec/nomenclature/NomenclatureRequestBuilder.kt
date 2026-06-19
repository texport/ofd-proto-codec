package kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.codec.nomenclature

import kz.kazakhtelecom.proto.v203.Nomenclature
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull

/**
 * Сборщик NomenclatureRequest из JSON-структуры.
 */
class NomenclatureRequestBuilder {
    /**
     * Строит NomenclatureRequest из JSON-объекта payload.
     */
    fun build(payload: JsonObject): Nomenclature.NomenclatureRequest {
        val nomenclatureJson = payload["nomenclature"] as? JsonObject
            ?: throw IllegalArgumentException("Missing nomenclature")

        val currentVersion = readInt(nomenclatureJson, "currentVersion")
        val builder = Nomenclature.NomenclatureRequest.newBuilder()
        if (currentVersion != null) {
            builder.setCurrentVersion(currentVersion)
        }

        readString(nomenclatureJson, "barcode")?.let { builder.setBarcode(it) }

        require(currentVersion != null || builder.hasBarcode()) {
            "Missing currentVersion or barcode"
        }

        return builder.build()
    }

    /**
     * Читает int, если поле присутствует.
     */
    private fun readInt(json: JsonObject, key: String): Int? {
        val element = json[key] as? JsonPrimitive ?: return null
        return element.intOrNull
    }

    /**
     * Читает строку, если поле присутствует.
     */
    private fun readString(json: JsonObject, key: String): String? {
        val element = json[key] as? JsonPrimitive ?: return null
        return if (element.isString) element.content else null
    }
}
