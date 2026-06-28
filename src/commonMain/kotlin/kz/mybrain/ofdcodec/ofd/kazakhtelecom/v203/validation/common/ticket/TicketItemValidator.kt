package kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.validation.common.ticket

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull
import kz.mybrain.ofdcodec.domain.model.ValidationError
import kz.mybrain.ofdcodec.domain.validation.ValidationUtils
import kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.validation.common.MoneyValidator
import kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.validation.common.enums.TicketItemTypeEnumValidator

/**
 * Валидация Item для TicketRequest.
 *
 * Проверяет обязательность полей и соответствие структуре для каждого типа позиции:
 * commodity / stornoCommodity / markup / stornoMarkup / discount / stornoDiscount.
 */
internal class TicketItemValidator {
    private val itemTypeValidator = TicketItemTypeEnumValidator()
    private val moneyValidator = MoneyValidator()
    private val taxValidator = TicketTaxValidator()
    private val modifierValidator = TicketModifierValidator()

    /**
     * Валидирует список items.
     * Проверяет JSON и собирает все ошибки по полям без остановки на первой.
     */
    fun validateList(container: JsonObject, key: String, path: String): List<ValidationError> {
        val errors = mutableListOf<ValidationError>()
        val items = container[key] ?: return errors
        val array = items as? JsonArray
        if (array == null) {
            errors.add(ValidationUtils.invalidType(path))
            return errors
        }
        array.forEachIndexed { index, item ->
            val itemPath = "$path[$index]"
            val obj = item as? JsonObject
            if (obj == null) {
                errors.add(ValidationUtils.invalidType(itemPath))
            } else {
                errors.addAll(validate(obj, itemPath))
            }
        }
        return errors
    }

    /**
     * Валидирует один Item.
     * Для commodity/stornoCommodity требует sectionCode, quantity, price, sum, measureUnitCode.
     */
    fun validate(item: JsonObject, path: String): List<ValidationError> {
        val errors = mutableListOf<ValidationError>()
        errors.addAll(itemTypeValidator.validate(item, "type", "$path.type"))

        val typeValue = (item["type"] as? JsonPrimitive)?.content
        when (typeValue) {
            "ITEM_TYPE_COMMODITY" -> validateCommodity(item, "$path.commodity", errors)
            "ITEM_TYPE_STORNO_COMMODITY" -> validateStornoCommodity(item, "$path.stornoCommodity", errors)
            "ITEM_TYPE_MARKUP" -> errors.addAll(modifierValidator.validate(item, "markup", "$path.markup"))
            "ITEM_TYPE_STORNO_MARKUP" -> errors.addAll(
                modifierValidator.validate(item, "stornoMarkup", "$path.stornoMarkup")
            )
            "ITEM_TYPE_DISCOUNT" -> errors.addAll(modifierValidator.validate(item, "discount", "$path.discount"))
            "ITEM_TYPE_STORNO_DISCOUNT" -> errors.addAll(
                modifierValidator.validate(item, "stornoDiscount", "$path.stornoDiscount")
            )
        }

        return errors
    }

    /**
     * Валидирует Commodity.
     *
     * Правило протокола: обязателен либо name, либо code.
     */
    private fun validateCommodity(container: JsonObject, path: String, errors: MutableList<ValidationError>) {
        val commodity = container["commodity"] as? JsonObject
        if (commodity == null) {
            errors.add(ValidationUtils.missingField(path))
            return
        }

        val name = commodity["name"] as? JsonPrimitive
        if (name != null && (!name.isString || name.content.isBlank())) {
            errors.add(ValidationUtils.invalidValue("$path.name"))
        }
        if (name == null) {
            ValidationUtils.requireLongInRange(commodity, "code", 0, Long.MAX_VALUE, "$path.code", errors)
        }

        ValidationUtils.requireNonBlankString(commodity, "sectionCode", "$path.sectionCode", errors)
        ValidationUtils.requireLongInRange(commodity, "quantity", 0, Long.MAX_VALUE, "$path.quantity", errors)
        errors.addAll(moneyValidator.validate(commodity, "price", "$path.price"))
        errors.addAll(moneyValidator.validate(commodity, "sum", "$path.sum"))
        validateTaxes(commodity, "$path.taxes", errors)

        validateExciseStampList(commodity, "$path.listExciseStamp", errors)
        ValidationUtils.optionalNonBlankString(commodity, "physicalLabel", "$path.physicalLabel", errors)
        ValidationUtils.optionalNonBlankString(commodity, "productId", "$path.productId", errors)
        ValidationUtils.optionalNonBlankString(commodity, "barcode", "$path.barcode", errors)
        ValidationUtils.optionalNonBlankString(commodity, "ntin", "$path.ntin", errors)
        ValidationUtils.requireNonBlankString(commodity, "measureUnitCode", "$path.measureUnitCode", errors)
    }

    /**
     * Валидирует StornoCommodity.
     *
     * Правило протокола: name опционален, остальные поля как у Commodity.
     */
    private fun validateStornoCommodity(container: JsonObject, path: String, errors: MutableList<ValidationError>) {
        val storno = container["stornoCommodity"] as? JsonObject
        if (storno == null) {
            errors.add(ValidationUtils.missingField(path))
            return
        }

        val name = storno["name"] as? JsonPrimitive
        if (name != null && (!name.isString || name.content.isBlank())) {
            errors.add(ValidationUtils.invalidValue("$path.name"))
        }

        ValidationUtils.requireNonBlankString(storno, "sectionCode", "$path.sectionCode", errors)
        ValidationUtils.requireLongInRange(storno, "quantity", 0, Long.MAX_VALUE, "$path.quantity", errors)
        errors.addAll(moneyValidator.validate(storno, "price", "$path.price"))
        errors.addAll(moneyValidator.validate(storno, "sum", "$path.sum"))
        validateTaxes(storno, "$path.taxes", errors)

        validateExciseStampList(storno, "$path.listExciseStamp", errors)
        ValidationUtils.optionalNonBlankString(storno, "physicalLabel", "$path.physicalLabel", errors)
        ValidationUtils.optionalNonBlankString(storno, "productId", "$path.productId", errors)
        ValidationUtils.optionalNonBlankString(storno, "barcode", "$path.barcode", errors)
        ValidationUtils.optionalNonBlankString(storno, "ntin", "$path.ntin", errors)
        ValidationUtils.requireNonBlankString(storno, "measureUnitCode", "$path.measureUnitCode", errors)
    }

    /**
     * Валидирует список налогов.
     * Дополнительно проверяет уникальность percent в списке.
     */
    private fun validateTaxes(container: JsonObject, path: String, errors: MutableList<ValidationError>) {
        val taxes = container["taxes"] ?: return
        val array = taxes as? JsonArray
        if (array == null) {
            errors.add(ValidationUtils.invalidType(path))
            return
        }
        val percents = array.mapNotNull { (it as? JsonObject)?.get("percent") as? JsonPrimitive }
            .mapNotNull { primitive -> primitive.intOrNull }
        if (percents.size != percents.toSet().size) {
            errors.add(ValidationUtils.invalidValue(path))
        }
        array.forEachIndexed { index, tax ->
            val taxObj = tax as? JsonObject
            if (taxObj == null) {
                errors.add(ValidationUtils.invalidType("$path[$index]"))
            } else {
                errors.addAll(taxValidator.validateObject(taxObj, "$path[$index]"))
            }
        }
    }

    /**
     * Валидирует список акцизных марок.
     */
    private fun validateExciseStampList(
        container: JsonObject,
        path: String,
        errors: MutableList<ValidationError>
    ) {
        val list = container["listExciseStamp"] ?: return
        val array = list as? JsonArray
        if (array == null) {
            errors.add(ValidationUtils.invalidType(path))
            return
        }
        array.forEachIndexed { index, item ->
            val primitive = item as? JsonPrimitive
            if (primitive == null || !primitive.isString || primitive.content.isBlank()) {
                errors.add(ValidationUtils.invalidValue("$path[$index]"))
            }
        }
    }
}
