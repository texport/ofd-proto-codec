package kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.validation.response

import kz.mybrain.ofdcodec.domain.model.CommandType
import kz.mybrain.ofdcodec.domain.model.ValidationError
import kz.mybrain.ofdcodec.domain.port.Validator
import kz.mybrain.ofdcodec.domain.validation.ValidationUtils
import kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.validation.common.DateTimeValidator
import kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.validation.common.MoneyValidator
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.intOrNull

/**
 * Валидатор ответа для COMMAND_NOMENCLATURE.
 */
class ResponseValidatorNomenclature : Validator {
    private val dateTimeValidator = DateTimeValidator()
    private val moneyValidator = MoneyValidator()

    /**
     * Проверяет JSON и собирает все ошибки по полям без остановки на первой.
     */
    override fun validate(commandType: CommandType, json: JsonObject): List<ValidationError> {
        val errors = mutableListOf<ValidationError>()

        // Базовый результат ответа сервера обязателен для любой команды.
        val result = json["result"] as? JsonObject
        if (result == null) {
            errors.add(ValidationUtils.missingField("$.payload.result"))
            return errors
        }
        ValidationUtils.requireIntInRange(
            result,
            "resultCode",
            0,
            Int.MAX_VALUE,
            "$.payload.result.resultCode",
            errors
        )

        val resultCodeValue = (result["resultCode"] as? JsonPrimitive)?.intOrNull
        if (resultCodeValue != null && resultCodeValue != 0) {
            // При ошибке сервера специфическая часть ответа может отсутствовать.
            return errors
        }

        // Объект nomenclature обязателен в ответе на COMMAND_NOMENCLATURE.
        val nomenclature = json["nomenclature"] as? JsonObject
        if (nomenclature == null) {
            errors.add(ValidationUtils.missingField("$.payload.nomenclature"))
            return errors
        }

        // version обязателен, uint32 (>= 0).
        ValidationUtils.requireIntInRange(
            nomenclature,
            "version",
            0,
            Int.MAX_VALUE,
            "$.payload.nomenclature.version",
            errors
        )

        // createdTime опционален, но если есть — DateTime.
        val createdTime = nomenclature["createdTime"]
        if (createdTime != null) {
            if (createdTime !is JsonObject) {
                errors.add(ValidationUtils.invalidType("$.payload.nomenclature.createdTime"))
            } else {
                errors.addAll(dateTimeValidator.validate(nomenclature, "createdTime", "$.payload.nomenclature.createdTime"))
            }
        }

        // result обязателен, содержит code и name.
        val nomenclatureResult = nomenclature["result"] as? JsonObject
        if (nomenclatureResult == null) {
            errors.add(ValidationUtils.missingField("$.payload.nomenclature.result"))
        } else {
            ValidationUtils.requireIntInRange(
                nomenclatureResult,
                "code",
                0,
                Int.MAX_VALUE,
                "$.payload.nomenclature.result.code",
                errors
            )
            ValidationUtils.requireNonBlankString(
                nomenclatureResult,
                "name",
                "$.payload.nomenclature.result.name",
                errors
            )
        }

        // elements опционален, но если есть — массив элементов.
        val elements = nomenclature["elements"]
        if (elements != null) {
            if (elements !is JsonArray) {
                errors.add(ValidationUtils.invalidType("$.payload.nomenclature.elements"))
            } else {
                elements.forEachIndexed { index, element ->
                    val elementPath = "$.payload.nomenclature.elements[$index]"
                    val elementObj = element as? JsonObject
                    if (elementObj == null) {
                        errors.add(ValidationUtils.invalidType(elementPath))
                        return@forEachIndexed
                    }
                    validateElement(elementObj, elementPath, errors)
                }
            }
        }

        return errors
    }

    /**
     * Проверяет один элемент номенклатуры.
     */
    private fun validateElement(element: JsonObject, path: String, errors: MutableList<ValidationError>) {
        // type обязателен и должен быть строкой.
        val typeElement = element["type"] as? JsonPrimitive
        if (typeElement == null) {
            errors.add(ValidationUtils.missingField("$path.type"))
        } else if (!typeElement.isString) {
            errors.add(ValidationUtils.invalidType("$path.type"))
        }

        // title обязателен и должен быть строкой.
        ValidationUtils.requireNonBlankString(element, "title", "$path.title", errors)
        // titleKk опционален, но если есть — строка.
        ValidationUtils.optionalNonBlankString(element, "titleKk", "$path.titleKk", errors)
        // id обязателен, uint64 (>= 0).
        ValidationUtils.requireLongInRange(element, "id", 0, Long.MAX_VALUE, "$path.id", errors)
        // parentGroupId опционален, uint64.
        if (element["parentGroupId"] != null) {
            ValidationUtils.requireLongInRange(element, "parentGroupId", 0, Long.MAX_VALUE, "$path.parentGroupId", errors)
        }

        val isItemType = typeElement?.isString == true && typeElement.content == "ITEM"
        if (isItemType) {
            val item = element["item"] as? JsonObject
            if (item == null) {
                errors.add(ValidationUtils.missingField("$path.item"))
            } else {
                validateItem(item, "$path.item", errors)
            }
        }
    }

    /**
     * Проверяет товарную позицию номенклатуры.
     */
    private fun validateItem(item: JsonObject, path: String, errors: MutableList<ValidationError>) {
        // article/barcode/description опциональны, но если есть — строки.
        ValidationUtils.optionalNonBlankString(item, "article", "$path.article", errors)
        ValidationUtils.optionalNonBlankString(item, "barcode", "$path.barcode", errors)
        ValidationUtils.optionalNonBlankString(item, "description", "$path.description", errors)

        // purchasePrice/sellPrice могут отсутствовать в ответе сервера, проверяем только если поля есть.
        if (item["purchasePrice"] != null) {
            errors.addAll(moneyValidator.validate(item, "purchasePrice", "$path.purchasePrice"))
        }
        if (item["sellPrice"] != null) {
            errors.addAll(moneyValidator.validate(item, "sellPrice", "$path.sellPrice"))
        }

        // discountPercent/markupPercent опциональны, uint32.
        if (item["discountPercent"] != null) {
            ValidationUtils.requireIntInRange(item, "discountPercent", 0, Int.MAX_VALUE, "$path.discountPercent", errors)
        }
        if (item["markupPercent"] != null) {
            ValidationUtils.requireIntInRange(item, "markupPercent", 0, Int.MAX_VALUE, "$path.markupPercent", errors)
        }

        // discountSum/markupSum опциональны, Money.
        if (item["discountSum"] != null) {
            errors.addAll(moneyValidator.validate(item, "discountSum", "$path.discountSum"))
        }
        if (item["markupSum"] != null) {
            errors.addAll(moneyValidator.validate(item, "markupSum", "$path.markupSum"))
        }

        // taxes опционален, но если есть — массив Tax.
        val taxes = item["taxes"]
        if (taxes != null) {
            if (taxes !is JsonArray) {
                errors.add(ValidationUtils.invalidType("$path.taxes"))
            } else {
                taxes.forEachIndexed { index, tax ->
                    val taxPath = "$path.taxes[$index]"
                    val taxObj = tax as? JsonObject
                    if (taxObj == null) {
                        errors.add(ValidationUtils.invalidType(taxPath))
                        return@forEachIndexed
                    }
                    ValidationUtils.requireIntInRange(taxObj, "taxationType", 0, Int.MAX_VALUE, "$taxPath.taxationType", errors)
                    ValidationUtils.requireIntInRange(taxObj, "taxType", 0, Int.MAX_VALUE, "$taxPath.taxType", errors)
                    ValidationUtils.requireIntInRange(taxObj, "taxPercent", 0, Int.MAX_VALUE, "$taxPath.taxPercent", errors)
                }
            }
        }

        // measureCount опционален, uint32.
        if (item["measureCount"] != null) {
            ValidationUtils.requireIntInRange(item, "measureCount", 0, Int.MAX_VALUE, "$path.measureCount", errors)
        }
        // measureTitle опционален, строка.
        ValidationUtils.optionalNonBlankString(item, "measureTitle", "$path.measureTitle", errors)
        // measureFractional опционален, boolean.
        val measureFractional = item["measureFractional"]
        if (measureFractional != null && (measureFractional !is JsonPrimitive || measureFractional.booleanOrNull == null)) {
            errors.add(ValidationUtils.invalidType("$path.measureFractional"))
        }
        // measureUnitCode опционален, строка.
        ValidationUtils.optionalNonBlankString(item, "measureUnitCode", "$path.measureUnitCode", errors)
        // ntin опционален, строка.
        ValidationUtils.optionalNonBlankString(item, "ntin", "$path.ntin", errors)
        // isMarkedeac опционален, boolean.
        val isMarkedeac = item["isMarkedeac"]
        if (isMarkedeac != null && (isMarkedeac !is JsonPrimitive || isMarkedeac.booleanOrNull == null)) {
            errors.add(ValidationUtils.invalidType("$path.isMarkedeac"))
        }
        // isSocial опционален, boolean.
        val isSocial = item["isSocial"]
        if (isSocial != null && (isSocial !is JsonPrimitive || isSocial.booleanOrNull == null)) {
            errors.add(ValidationUtils.invalidType("$path.isSocial"))
        }
    }
}
