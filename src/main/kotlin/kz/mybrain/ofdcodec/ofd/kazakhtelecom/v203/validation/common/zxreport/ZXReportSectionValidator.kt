package kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.validation.common.zxreport

import kz.mybrain.ofdcodec.domain.model.ValidationError
import kz.mybrain.ofdcodec.domain.validation.ValidationUtils
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject

/**
 * Валидация Section внутри ZXReport.
 */
class ZXReportSectionValidator {
    private val operationValidator = ZXReportOperationValidator()

    /**
     * Валидирует список секций по ключу в контейнере.
     */
    fun validateList(container: JsonObject, key: String, path: String): List<ValidationError> {
        val errors = mutableListOf<ValidationError>()
        val sections = container[key] ?: return errors
        val array = sections as? JsonArray
        if (array == null) {
            errors.add(ValidationUtils.invalidType(path))
            return errors
        }
        array.forEachIndexed { index, section ->
            val sectionPath = "$path[$index]"
            val sectionObj = section as? JsonObject
            if (sectionObj == null) {
                errors.add(ValidationUtils.invalidType(sectionPath))
            } else {
                errors.addAll(validate(sectionObj, sectionPath))
            }
        }
        return errors
    }

    /**
     * Валидирует одну секцию.
     */
    fun validate(section: JsonObject, path: String): List<ValidationError> {
        val errors = mutableListOf<ValidationError>()
        ValidationUtils.requireNonBlankString(section, "sectionCode", "$path.sectionCode", errors)
        val operations = ValidationUtils.requireArray(section, "operations", "$path.operations", errors)
        if (operations != null) {
            errors.addAll(operationValidator.validateList(section, "operations", "$path.operations"))
        }
        return errors
    }
}
