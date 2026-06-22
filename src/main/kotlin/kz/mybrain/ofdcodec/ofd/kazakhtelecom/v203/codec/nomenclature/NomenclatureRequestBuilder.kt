package kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.codec.nomenclature

import kotlinx.serialization.json.JsonObject
import kz.kazakhtelecom.proto.v203.Nomenclature
import kz.mybrain.ofdcodec.infrastructure.json.readInt
import kz.mybrain.ofdcodec.infrastructure.json.readString

/**
 * Сборщик NomenclatureRequest из JSON-структуры.
 */
internal class NomenclatureRequestBuilder {
    /**
     * Строит NomenclatureRequest из JSON-объекта payload.
     */
    fun build(payload: JsonObject): Nomenclature.NomenclatureRequest {
        val nomenclatureJson = payload["nomenclature"] as? JsonObject
            ?: throw IllegalArgumentException("Missing nomenclature / Отсутствует nomenclature / nomenclature өрісі жетіспейді")

        val currentVersion = nomenclatureJson.readInt("currentVersion")
        val builder = Nomenclature.NomenclatureRequest.newBuilder()
        if (currentVersion != null) {
            builder.setCurrentVersion(currentVersion)
        }

        nomenclatureJson.readString("barcode")?.let { builder.setBarcode(it) }

        require(currentVersion != null || builder.hasBarcode()) {
            "Missing currentVersion or barcode / Отсутствует currentVersion или barcode / currentVersion немесе barcode өрісі жетіспейді"
        }

        return builder.build()
    }
}
