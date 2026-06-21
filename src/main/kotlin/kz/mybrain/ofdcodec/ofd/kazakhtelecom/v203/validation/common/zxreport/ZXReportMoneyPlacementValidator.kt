package kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.validation.common.zxreport

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kz.mybrain.ofdcodec.domain.model.ValidationError
import kz.mybrain.ofdcodec.domain.validation.ValidationUtils
import kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.validation.common.MoneyValidator

/**
 * Валидация MoneyPlacement внутри ZXReport.
 */
class ZXReportMoneyPlacementValidator {
    private val moneyValidator = MoneyValidator()

    /**
     * Валидирует список операций внесения/снятия по ключу в контейнере.
     */
    fun validateList(container: JsonObject, key: String, path: String): List<ValidationError> {
        val errors = mutableListOf<ValidationError>()
        val placements = container[key] ?: return errors
        val array = placements as? JsonArray
        if (array == null) {
            errors.add(ValidationUtils.invalidType(path))
            return errors
        }
        array.forEachIndexed { index, placement ->
            val placementPath = "$path[$index]"
            val placementObj = placement as? JsonObject
            if (placementObj == null) {
                errors.add(ValidationUtils.invalidType(placementPath))
            } else {
                errors.addAll(validate(placementObj, placementPath))
            }
        }
        return errors
    }

    /**
     * Валидирует одну операцию внесения/снятия.
     */
    fun validate(placement: JsonObject, path: String): List<ValidationError> {
        val errors = mutableListOf<ValidationError>()
        ValidationUtils.requireNonBlankString(placement, "operation", "$path.operation", errors)
        ValidationUtils.requireIntInRange(
            placement,
            "operationsTotalCount",
            0,
            Int.MAX_VALUE,
            "$path.operationsTotalCount",
            errors
        )
        ValidationUtils.requireIntInRange(
            placement,
            "operationsCount",
            0,
            Int.MAX_VALUE,
            "$path.operationsCount",
            errors
        )
        errors.addAll(moneyValidator.validate(placement, "operationsSum", "$path.operationsSum"))
        ValidationUtils.requireIntInRange(placement, "offlineCount", 0, Int.MAX_VALUE, "$path.offlineCount", errors)
        return errors
    }
}
