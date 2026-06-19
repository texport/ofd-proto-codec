package kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.codec.service

import kz.kazakhtelecom.proto.v203.Service
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull

/**
 * Сборщик proto SecurityStats из JSON-структуры.
 */
class SecurityStatsBuilder {
    /**
     * Строит SecurityStats из JSON-объекта.
     */
    fun build(json: JsonObject): Service.ServiceRequest.SecurityStats {
        val geo = json["geoPosition"] as? JsonObject ?: throw IllegalArgumentException("Missing geoPosition")

        return Service.ServiceRequest.SecurityStats.newBuilder()
            .setGeoPosition(buildGeoPosition(geo))
            .build()
    }

    /**
     * Собирает GeoPosition из JSON.
     */
    private fun buildGeoPosition(json: JsonObject): Service.ServiceRequest.SecurityStats.GeoPosition {
        return Service.ServiceRequest.SecurityStats.GeoPosition.newBuilder()
            .setLatitude(readIntRequired(json, "latitude"))
            .setLongitude(readIntRequired(json, "longitude"))
            .setSource(readStringRequired(json, "source"))
            .build()
    }

    /**
     * Читает обязательное целое значение или выбрасывает ошибку.
     */
    private fun readIntRequired(json: JsonObject, key: String): Int {
        val element = json[key] as? JsonPrimitive ?: throw IllegalArgumentException("Missing $key")
        return element.intOrNull ?: throw IllegalArgumentException("Invalid type for $key")
    }

    /**
     * Читает обязательную строку или выбрасывает ошибку.
     */
    private fun readStringRequired(json: JsonObject, key: String): String {
        val element = json[key] as? JsonPrimitive ?: throw IllegalArgumentException("Missing $key")
        require(element.isString) { "Invalid type for $key" }
        return element.content
    }
}
