package kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.validation.common.zxreport

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kz.mybrain.ofdcodec.domain.model.ValidationError
import kz.mybrain.ofdcodec.domain.validation.ValidationUtils
import kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.validation.common.MoneyValidator
import kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.validation.common.enums.OperationTypeEnumValidator

/**
 * Валидация NonNullableSum внутри ZXReport.
 */
class ZXReportNonNullableSumValidator {
    private val operationTypeValidator = OperationTypeEnumValidator()
    private val moneyValidator = MoneyValidator()

    /**
     * Валидирует список необнуляемых сумм по ключу в контейнере.
     */
    fun validateList(container: JsonObject, key: String, path: String): List<ValidationError> {
        val errors = mutableListOf<ValidationError>()
        val sums = container[key] ?: return errors
        val array = sums as? JsonArray
        if (array == null) {
            errors.add(ValidationUtils.invalidType(path))
            return errors
        }
        array.forEachIndexed { index, sum ->
            val sumPath = "$path[$index]"
            val sumObj = sum as? JsonObject
            if (sumObj == null) {
                errors.add(ValidationUtils.invalidType(sumPath))
            } else {
                errors.addAll(validate(sumObj, sumPath))
            }
        }
        return errors
    }

    /**
     * Валидирует одну необнуляемую сумму.
     */
    fun validate(sum: JsonObject, path: String): List<ValidationError> {
        val errors = mutableListOf<ValidationError>()
        errors.addAll(operationTypeValidator.validate(sum, "operation", "$path.operation"))
        errors.addAll(moneyValidator.validate(sum, "sum", "$path.sum"))
        return errors
    }
}
