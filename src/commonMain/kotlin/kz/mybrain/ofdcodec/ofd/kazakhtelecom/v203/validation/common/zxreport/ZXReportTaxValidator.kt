package kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.validation.common.zxreport

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kz.mybrain.ofdcodec.domain.model.ValidationError
import kz.mybrain.ofdcodec.domain.validation.ValidationUtils

/**
 * Валидация Tax внутри ZXReport.
 */
internal class ZXReportTaxValidator {
    private val taxOperationValidator = ZXReportTaxOperationValidator()

    /**
     * Валидирует список налогов по ключу в контейнере.
     */
    fun validateList(container: JsonObject, key: String, path: String): List<ValidationError> {
        val errors = mutableListOf<ValidationError>()
        val taxes = container[key] ?: return errors
        val array = taxes as? JsonArray
        if (array == null) {
            errors.add(ValidationUtils.invalidType(path))
            return errors
        }
        array.forEachIndexed { index, tax ->
            val taxPath = "$path[$index]"
            val taxObj = tax as? JsonObject
            if (taxObj == null) {
                errors.add(ValidationUtils.invalidType(taxPath))
            } else {
                errors.addAll(validate(taxObj, taxPath))
            }
        }
        return errors
    }

    /**
     * Валидирует один налог.
     */
    fun validate(tax: JsonObject, path: String): List<ValidationError> {
        val errors = mutableListOf<ValidationError>()
        ValidationUtils.requireIntInRange(tax, "taxType", 0, Int.MAX_VALUE, "$path.taxType", errors)
        ValidationUtils.requireIntInRange(tax, "percent", 0, Int.MAX_VALUE, "$path.percent", errors)
        val operations = ValidationUtils.requireArray(tax, "operations", "$path.operations", errors)
        if (operations != null) {
            errors.addAll(taxOperationValidator.validateList(tax, "operations", "$path.operations"))
        }
        return errors
    }
}
