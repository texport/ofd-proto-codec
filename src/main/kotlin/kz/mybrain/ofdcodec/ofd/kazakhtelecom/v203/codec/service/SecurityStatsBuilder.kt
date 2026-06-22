package kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.codec.service

import kotlinx.serialization.json.JsonObject
import kz.kazakhtelecom.proto.v203.Service
import kz.mybrain.ofdcodec.infrastructure.json.readIntRequired
import kz.mybrain.ofdcodec.infrastructure.json.readStringRequired

/**
 * Сборщик proto SecurityStats из JSON-структуры.
 */
internal class SecurityStatsBuilder {
    /**
     * Строит SecurityStats из JSON-объекта.
     */
    fun build(json: JsonObject): Service.ServiceRequest.SecurityStats {
        val geo = json["geoPosition"] as? JsonObject ?: throw IllegalArgumentException("Missing geoPosition / Отсутствует geoPosition / geoPosition өрісі жетіспейді")

        return Service.ServiceRequest.SecurityStats.newBuilder()
            .setGeoPosition(buildGeoPosition(geo))
            .build()
    }

    /**
     * Собирает GeoPosition из JSON.
     */
    private fun buildGeoPosition(json: JsonObject): Service.ServiceRequest.SecurityStats.GeoPosition {
        return Service.ServiceRequest.SecurityStats.GeoPosition.newBuilder()
            .setLatitude(json.readIntRequired("latitude"))
            .setLongitude(json.readIntRequired("longitude"))
            .setSource(json.readStringRequired("source"))
            .build()
    }
}
