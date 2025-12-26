package kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.validation.request.service

import kz.mybrain.ofdcodec.domain.model.ValidationError
import kz.mybrain.ofdcodec.domain.validation.ValidationUtils
import kotlinx.serialization.json.JsonObject

/**
 * Валидация SecurityStats и GeoPosition для протокола Казахтелеком v203.
 */
class SecurityStatsValidator {
    /**
     * Валидирует securityStats и вложенную geoPosition.
     * Проверяет JSON и собирает все ошибки по полям без остановки на первой.
     */
    fun validate(security: JsonObject, basePath: String): List<ValidationError> {
        val errors = mutableListOf<ValidationError>()
        val geo = security["geoPosition"] as? JsonObject
        if (geo == null) {
            errors.add(ValidationUtils.missingField("$basePath.geoPosition"))
            return errors
        }

        // Широта обязательна и должна быть числом >= 0.
        ValidationUtils.requireIntInRange(geo, "latitude", 0, Int.MAX_VALUE, "$basePath.geoPosition.latitude", errors)
        // Долгота обязательна и должна быть числом >= 0.
        ValidationUtils.requireIntInRange(geo, "longitude", 0, Int.MAX_VALUE, "$basePath.geoPosition.longitude", errors)
        // Источник геолокации обязателен и должен быть строкой.
        ValidationUtils.requireNonBlankString(geo, "source", "$basePath.geoPosition.source", errors)

        return errors
    }
}
