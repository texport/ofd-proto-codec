package kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.validation.common.zxreport

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kz.mybrain.ofdcodec.domain.model.ValidationError
import kz.mybrain.ofdcodec.domain.validation.ValidationUtils
import kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.validation.common.MoneyValidator

/**
 * Валидация Revenue внутри ZXReport.
 */
internal class ZXReportRevenueValidator {
    private val moneyValidator = MoneyValidator()

    /**
     * Валидирует Revenue по ключу в контейнере.
     */
    fun validate(container: JsonObject, key: String, path: String): List<ValidationError> {
        val errors = mutableListOf<ValidationError>()
        val revenue = container[key] as? JsonObject
        if (revenue == null) {
            errors.add(ValidationUtils.missingField(path))
            return errors
        }
        errors.addAll(moneyValidator.validate(revenue, "sum", "$path.sum"))
        val isNegative = revenue["isNegative"]
        if (isNegative == null) {
            errors.add(ValidationUtils.missingField("$path.isNegative"))
        } else if (isNegative !is JsonPrimitive || isNegative.booleanOrNull == null) {
            errors.add(ValidationUtils.invalidType("$path.isNegative"))
        }
        return errors
    }
}
