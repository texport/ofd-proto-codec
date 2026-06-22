package kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.validation.common.zxreport

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kz.mybrain.ofdcodec.domain.model.ValidationError
import kz.mybrain.ofdcodec.domain.validation.ValidationUtils
import kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.validation.common.MoneyValidator
import kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.validation.common.enums.OperationTypeEnumValidator

/**
 * Валидация TaxOperation внутри ZXReport.Tax.
 */
internal class ZXReportTaxOperationValidator {
    private val operationTypeValidator = OperationTypeEnumValidator()
    private val moneyValidator = MoneyValidator()

    /**
     * Валидирует список операций налога по ключу в контейнере.
     */
    fun validateList(container: JsonObject, key: String, path: String): List<ValidationError> {
        val errors = mutableListOf<ValidationError>()
        val operations = container[key] ?: return errors
        val array = operations as? JsonArray
        if (array == null) {
            errors.add(ValidationUtils.invalidType(path))
            return errors
        }
        array.forEachIndexed { index, op ->
            val opPath = "$path[$index]"
            val opObj = op as? JsonObject
            if (opObj == null) {
                errors.add(ValidationUtils.invalidType(opPath))
            } else {
                errors.addAll(validate(opObj, opPath))
            }
        }
        return errors
    }

    /**
     * Валидирует одну операцию налога.
     */
    fun validate(operation: JsonObject, path: String): List<ValidationError> {
        val errors = mutableListOf<ValidationError>()
        errors.addAll(operationTypeValidator.validate(operation, "operation", "$path.operation"))
        errors.addAll(moneyValidator.validate(operation, "turnover", "$path.turnover"))
        errors.addAll(moneyValidator.validate(operation, "sum", "$path.sum"))
        errors.addAll(moneyValidator.validate(operation, "turnoverWithoutTax", "$path.turnoverWithoutTax"))
        return errors
    }
}
