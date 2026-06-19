package kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.validation.request.service

import kz.mybrain.ofdcodec.domain.model.ValidationError
import kz.mybrain.ofdcodec.domain.validation.ValidationUtils
import kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.validation.common.DateTimeValidator
import kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.validation.common.service.KkmRegInfoValidator
import kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.validation.common.service.OrgRegInfoValidator
import kotlinx.serialization.json.JsonObject

/**
 * Базовая валидация ServiceRequest для протокола Казахтелеком v203.
 * Обязательные поля берутся из документа протокола.
 */
class ServiceRequestValidator {
    private val dateTimeValidator = DateTimeValidator()
    private val kkmRegInfoValidator = KkmRegInfoValidator()
    private val orgRegInfoValidator = OrgRegInfoValidator()
    private val securityStatsValidator = SecurityStatsValidator()

    /**
     * Валидирует объект service и возвращает список всех ошибок.
     * Проверяет JSON и собирает все ошибки по полям без остановки на первой.
     */
    fun validate(service: JsonObject, basePath: String = "$.payload.service"): List<ValidationError> {
        val errors = mutableListOf<ValidationError>()

        // Проверяем обязательный флаг запроса регистрационной информации.
        ValidationUtils.requireBoolean(service, "getRegInfo", "$basePath.getRegInfo", errors)
        // Проверяем обязательный объект offlinePeriod и оба DateTime внутри.
        ValidationUtils.requireObject(service, "offlinePeriod", "$basePath.offlinePeriod", errors)?.let { offline ->
            // Дата и время начала автономного режима.
            errors.addAll(dateTimeValidator.validate(offline, "beginTime", "$basePath.offlinePeriod.beginTime"))
            // Дата и время окончания автономного режима.
            errors.addAll(dateTimeValidator.validate(offline, "endTime", "$basePath.offlinePeriod.endTime"))
        }
        // Проверяем обязательный блок securityStats.
        ValidationUtils.requireObject(service, "securityStats", "$basePath.securityStats", errors)?.let { security ->
            // Внутри обязателен geoPosition и его поля.
            errors.addAll(securityStatsValidator.validate(security, "$basePath.securityStats"))
        }
        // Проверяем обязательный блок regInfo.
        ValidationUtils.requireObject(service, "regInfo", "$basePath.regInfo", errors)?.let { regInfo ->
            // Обязательный kkm внутри regInfo.
            ValidationUtils.requireObject(regInfo, "kkm", "$basePath.regInfo.kkm", errors)?.let { kkm ->
                // Проверяем идентификаторы ККМ.
                errors.addAll(kkmRegInfoValidator.validate(kkm, "$basePath.regInfo.kkm"))
            }
            // Обязательный org внутри regInfo.
            ValidationUtils.requireObject(regInfo, "org", "$basePath.regInfo.org", errors)?.let { org ->
                // Проверяем регистрационные данные организации.
                errors.addAll(orgRegInfoValidator.validate(org, "$basePath.regInfo.org"))
            }
        }

        return errors
    }

}
