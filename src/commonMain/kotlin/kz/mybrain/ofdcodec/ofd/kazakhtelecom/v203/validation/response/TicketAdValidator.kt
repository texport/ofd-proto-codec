package kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.validation.response

import kotlinx.serialization.json.JsonObject
import kz.mybrain.ofdcodec.domain.model.ValidationError
import kz.mybrain.ofdcodec.domain.validation.ValidationUtils

/**
 * Валидация TicketAd для ответов текущего provider module v203.
 */
internal class TicketAdValidator {
    private val infoValidator = TicketAdInfoValidator()

    /**
     * Валидирует рекламный блок: info и текст.
     * Проверяет JSON и собирает все ошибки по полям без остановки на первой.
     */
    fun validate(ad: JsonObject, basePath: String): List<ValidationError> {
        val errors = mutableListOf<ValidationError>()
        // В TicketAd обязателен блок info.
        val info = ValidationUtils.requireObject(ad, "info", "$basePath.info", errors)
        // В TicketAd обязателен текст рекламы.
        ValidationUtils.requireNonBlankString(ad, "text", "$basePath.text", errors)
        if (info != null) {
            // Проверяем тип и версию блока.
            errors.addAll(infoValidator.validate(info, "$basePath.info"))
        }
        return errors
    }
}
