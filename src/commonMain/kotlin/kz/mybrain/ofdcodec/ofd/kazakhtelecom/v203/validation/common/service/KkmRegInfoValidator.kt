package kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.validation.common.service

import kotlinx.serialization.json.JsonObject
import kz.mybrain.ofdcodec.domain.model.ValidationError
import kz.mybrain.ofdcodec.domain.validation.ValidationUtils

/**
 * Валидация KkmRegInfo для протокола Казахтелеком v203.
 */
internal class KkmRegInfoValidator {
    /**
     * Валидирует обязательные и опциональные поля KkmRegInfo.
     * Проверяет JSON и собирает все ошибки по полям без остановки на первой.
     */
    fun validate(kkm: JsonObject, basePath: String): List<ValidationError> {
        val errors = mutableListOf<ValidationError>()

        // Идентификатор ККМ от КГД — обязательная строка.
        ValidationUtils.requireNonBlankString(kkm, "fnsKkmId", "$basePath.fnsKkmId", errors)
        // Заводской номер ККМ — обязательная строка.
        ValidationUtils.requireNonBlankString(kkm, "serialNumber", "$basePath.serialNumber", errors)
        // Внутренний идентификатор ККМ — обязательная строка.
        ValidationUtils.requireNonBlankString(kkm, "kkmId", "$basePath.kkmId", errors)

        // Регистрационный номер точки приема платежей — опциональная строка.
        ValidationUtils.optionalNonBlankString(kkm, "pointOfPaymentNumber", "$basePath.pointOfPaymentNumber", errors)
        // Номер платежного терминала — опциональная строка.
        ValidationUtils.optionalNonBlankString(kkm, "terminalNumber", "$basePath.terminalNumber", errors)

        return errors
    }
}
