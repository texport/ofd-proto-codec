package kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.codec.service

import kotlinx.serialization.json.JsonObject
import kz.kazakhtelecom.proto.v203.*
import kz.mybrain.ofdcodec.infrastructure.json.readIntRequired
import kz.mybrain.ofdcodec.infrastructure.json.readStringRequired

/**
 * Сборщик proto SecurityStats из JSON-структуры.
 */
internal class SecurityStatsBuilder {
    fun build(json: JsonObject): ServiceRequest.SecurityStats {
        val geo = json["geoPosition"] as? JsonObject ?: throw IllegalArgumentException("Missing geoPosition / Отсутствует geoPosition / geoPosition өрісі жетіспейді")

        return ServiceRequest.SecurityStats(
            geo_position = buildGeoPosition(geo)
        )
    }

    /**
     * Собирает GeoPosition из JSON.
     */
    private fun buildGeoPosition(json: JsonObject): ServiceRequest.SecurityStats.GeoPosition {
        return ServiceRequest.SecurityStats.GeoPosition(
            latitude = json.readIntRequired("latitude"),
            longitude = json.readIntRequired("longitude"),
            source = json.readStringRequired("source")
        )
    }
}
