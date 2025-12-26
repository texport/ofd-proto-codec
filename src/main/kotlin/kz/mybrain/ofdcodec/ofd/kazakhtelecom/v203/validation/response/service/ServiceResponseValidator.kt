package kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.validation.response.service

import kz.mybrain.ofdcodec.domain.model.ValidationError
import kz.mybrain.ofdcodec.domain.validation.ValidationUtils
import kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.validation.common.service.KkmRegInfoValidator
import kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.validation.common.service.OrgRegInfoValidator
import kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.validation.common.service.PosRegInfoValidator
import kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.validation.response.TicketAdValidator
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject

/**
 * Валидация ServiceResponse для ответов ОФД Казахтелеком v203.
 */
class ServiceResponseValidator {
    private val kkmValidator = KkmRegInfoValidator()
    private val orgValidator = OrgRegInfoValidator()
    private val posValidator = PosRegInfoValidator()
    private val ticketAdValidator = TicketAdValidator()

    /**
     * Валидирует структуру service в ответе сервера.
     * Проверяет JSON и собирает все ошибки по полям без остановки на первой.
     */
    fun validate(service: JsonObject, basePath: String = "$.payload.service"): List<ValidationError> {
        val errors = mutableListOf<ValidationError>()

        // В ответе service.regInfo обязателен.
        val regInfo = ValidationUtils.requireObject(service, "regInfo", "$basePath.regInfo", errors)
        if (regInfo != null) {
            // Внутри regInfo обязательный kkm.
            val kkm = ValidationUtils.requireObject(regInfo, "kkm", "$basePath.regInfo.kkm", errors)
            if (kkm != null) {
                // Проверяем идентификаторы ККМ.
                errors.addAll(kkmValidator.validate(kkm, "$basePath.regInfo.kkm"))
            }

            // Внутри regInfo обязательный org.
            val org = ValidationUtils.requireObject(regInfo, "org", "$basePath.regInfo.org", errors)
            if (org != null) {
                // Проверяем регистрационные данные организации.
                errors.addAll(orgValidator.validate(org, "$basePath.regInfo.org"))
            }

            // pos опционален, но если есть — валидируем все поля торговой точки.
            val posElement = regInfo["pos"]
            if (posElement != null) {
                val pos = posElement as? JsonObject
                if (pos == null) {
                    errors.add(ValidationUtils.invalidType("$basePath.regInfo.pos"))
                } else {
                    errors.addAll(posValidator.validate(pos, "$basePath.regInfo.pos"))
                }
            }
        }

        // ticketAds опционален, но если есть — должен быть массивом объектов TicketAd.
        val ticketAds = service["ticketAds"]
        if (ticketAds != null) {
            val array = ticketAds as? JsonArray
            if (array == null) {
                errors.add(ValidationUtils.invalidType("$basePath.ticketAds"))
            } else {
                array.forEachIndexed { index, element ->
                    val ad = element as? JsonObject
                    if (ad == null) {
                        errors.add(ValidationUtils.invalidType("$basePath.ticketAds[$index]"))
                    } else {
                        // Проверяем TicketAd: info + text.
                        errors.addAll(ticketAdValidator.validate(ad, "$basePath.ticketAds[$index]"))
                    }
                }
            }
        }

        return errors
    }
}
