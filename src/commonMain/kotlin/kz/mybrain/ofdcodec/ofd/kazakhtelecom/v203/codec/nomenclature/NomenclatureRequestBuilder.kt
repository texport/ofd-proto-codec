package kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.codec.nomenclature

import kotlinx.serialization.json.JsonObject
import kz.kazakhtelecom.proto.v203.*
import kz.mybrain.ofdcodec.infrastructure.json.readInt
import kz.mybrain.ofdcodec.infrastructure.json.readString

/**
 * Сборщик NomenclatureRequest из JSON-структуры.
 */
internal class NomenclatureRequestBuilder {
    fun build(payload: JsonObject): NomenclatureRequest {
        val nomenclatureJson = payload["nomenclature"] as? JsonObject
            ?: throw IllegalArgumentException("Missing nomenclature / Отсутствует nomenclature / nomenclature өрісі жетіспейді")

        val currentVersion = nomenclatureJson.readInt("currentVersion")
        val barcode = nomenclatureJson.readString("barcode")

        require(currentVersion != null || barcode != null) {
            "Missing currentVersion or barcode / Отсутствует currentVersion или barcode / currentVersion немесе barcode өрісі жетіспейді"
        }

        return NomenclatureRequest(
            current_version = currentVersion,
            barcode = barcode
        )
    }
}
