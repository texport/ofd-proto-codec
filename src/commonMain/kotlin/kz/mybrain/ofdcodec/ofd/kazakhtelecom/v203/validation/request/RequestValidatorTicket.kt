package kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.validation.request

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull
import kz.mybrain.ofdcodec.domain.model.CommandType
import kz.mybrain.ofdcodec.domain.model.ValidationError
import kz.mybrain.ofdcodec.domain.port.Validator
import kz.mybrain.ofdcodec.domain.validation.ValidationUtils
import kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.validation.common.DateTimeValidator
import kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.validation.common.OperatorValidator
import kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.validation.common.enums.OperationTypeEnumValidator
import kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.validation.common.ticket.TicketAmountsValidator
import kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.validation.common.ticket.TicketExtensionOptionsValidator
import kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.validation.common.ticket.TicketItemValidator
import kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.validation.common.ticket.TicketParentTicketValidator
import kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.validation.common.ticket.TicketPaymentValidator
import kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.validation.common.ticket.TicketTaxValidator
import kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.validation.request.service.ServiceRequestValidator

/**
 * Валидатор запроса для COMMAND_TICKET.
 *
 * Проверяет структуру чека согласно протоколу v203:
 * - обязательность service и ticket;
 * - наличие operation/dateTime/operator;
 * - список items обязателен и непустой, каждый элемент соответствует своему type;
 * - payments опциональны, но типы платежей не должны повторяться;
 * - taxes либо на уровне чека, либо на уровне позиций, но не одновременно;
 * - amounts обязательны, при оплате наличными требуются taken и change;
 * - parentTicket обязателен для возвратов (BUY_RETURN/SELL_RETURN);
 * - domain игнорируется (в v203 не используется).
 */
internal class RequestValidatorTicket : Validator {
    private val serviceValidator = ServiceRequestValidator()
    private val operationTypeValidator = OperationTypeEnumValidator()
    private val dateTimeValidator = DateTimeValidator()
    private val operatorValidator = OperatorValidator()
    private val itemValidator = TicketItemValidator()
    private val paymentValidator = TicketPaymentValidator()
    private val taxValidator = TicketTaxValidator()
    private val amountsValidator = TicketAmountsValidator()
    private val extensionOptionsValidator = TicketExtensionOptionsValidator()
    private val parentTicketValidator = TicketParentTicketValidator()

    /**
     * Проверяет JSON и собирает все ошибки по полям без остановки на первой.
     * Возвращает полный список нарушений, чтобы пользователь увидел все проблемы сразу.
     */
    override fun validate(commandType: CommandType, json: JsonObject): List<ValidationError> {
        val errors = mutableListOf<ValidationError>()

        val service = json["service"] as? JsonObject
        if (service == null) {
            errors.add(ValidationUtils.missingField("$.payload.service"))
            return errors
        }
        errors.addAll(serviceValidator.validate(service, "$.payload.service"))

        val ticket = json["ticket"] as? JsonObject
        if (ticket == null) {
            errors.add(ValidationUtils.missingField("$.payload.ticket"))
            return errors
        }

        errors.addAll(operationTypeValidator.validate(ticket, "operation", "$.payload.ticket.operation"))
        errors.addAll(dateTimeValidator.validate(ticket, "dateTime", "$.payload.ticket.dateTime"))
        errors.addAll(operatorValidator.validate(ticket, "operator", "$.payload.ticket.operator"))

        // domain не используется, поле игнорируется, если передано.

        val itemsElement = ticket["items"]
        if (itemsElement == null) {
            errors.add(ValidationUtils.missingField("$.payload.ticket.items"))
            return errors
        }
        if (itemsElement !is kotlinx.serialization.json.JsonArray || itemsElement.isEmpty()) {
            errors.add(ValidationUtils.invalidValue("$.payload.ticket.items"))
        } else {
            errors.addAll(itemValidator.validateList(ticket, "items", "$.payload.ticket.items"))
        }

        val paymentsElement = ticket["payments"]
        if (paymentsElement != null) {
            if (paymentsElement !is kotlinx.serialization.json.JsonArray) {
                errors.add(ValidationUtils.invalidType("$.payload.ticket.payments"))
            } else {
                paymentsElement.forEachIndexed { index, element ->
                    val payment = element as? JsonObject
                    if (payment == null) {
                        errors.add(ValidationUtils.invalidType("$.payload.ticket.payments[$index]"))
                    } else {
                        errors.addAll(paymentValidator.validateObject(payment, "$.payload.ticket.payments[$index]"))
                    }
                }
                val types = paymentsElement
                    .mapNotNull { (it as? JsonObject)?.get("type") as? JsonPrimitive }
                    .mapNotNull { primitive -> if (primitive.isString) primitive.content else null }
                if (types.size != types.toSet().size) {
                    errors.add(ValidationUtils.invalidValue("$.payload.ticket.payments"))
                }
            }
        }

        val taxesElement = ticket["taxes"]
        if (taxesElement != null) {
            if (taxesElement !is kotlinx.serialization.json.JsonArray) {
                errors.add(ValidationUtils.invalidType("$.payload.ticket.taxes"))
            } else {
                val percents = taxesElement
                    .mapNotNull { (it as? JsonObject)?.get("percent") as? JsonPrimitive }
                    .mapNotNull { primitive -> primitive.intOrNull }
                if (percents.size != percents.toSet().size) {
                    errors.add(ValidationUtils.invalidValue("$.payload.ticket.taxes"))
                }
                taxesElement.forEachIndexed { index, _ ->
                    errors.addAll(taxValidator.validate(ticket, "taxes[$index]", "$.payload.ticket.taxes[$index]"))
                }
            }
        }

        errors.addAll(amountsValidator.validate(ticket, "amounts", "$.payload.ticket.amounts"))

        val extensionOptions = ticket["extensionOptions"]
        if (extensionOptions is JsonObject) {
            errors.addAll(
                extensionOptionsValidator.validate(ticket, "extensionOptions", "$.payload.ticket.extensionOptions")
            )
        } else if (extensionOptions != null) {
            errors.add(ValidationUtils.invalidType("$.payload.ticket.extensionOptions"))
        }

        if (ticket["offlineTicketNumber"] != null) {
            ValidationUtils.requireIntInRange(
                ticket,
                "offlineTicketNumber",
                0,
                Int.MAX_VALUE,
                "$.payload.ticket.offlineTicketNumber",
                errors
            )
        }
        ValidationUtils.optionalNonBlankString(ticket, "printedTicket", "$.payload.ticket.printedTicket", errors)
        if (ticket["frShiftNumber"] != null) {
            ValidationUtils.requireIntInRange(
                ticket,
                "frShiftNumber",
                0,
                Int.MAX_VALUE,
                "$.payload.ticket.frShiftNumber",
                errors
            )
        }
        if (ticket["shiftDocumentNumber"] != null) {
            ValidationUtils.requireIntInRange(
                ticket,
                "shiftDocumentNumber",
                0,
                Int.MAX_VALUE,
                "$.payload.ticket.shiftDocumentNumber",
                errors
            )
        }
        if (ticket["printedDocumentNumber"] != null) {
            ValidationUtils.requireLongInRange(
                ticket,
                "printedDocumentNumber",
                0,
                Long.MAX_VALUE,
                "$.payload.ticket.printedDocumentNumber",
                errors
            )
        }

        val parentTicket = ticket["parentTicket"]
        if (parentTicket is JsonObject) {
            errors.addAll(parentTicketValidator.validate(ticket, "parentTicket", "$.payload.ticket.parentTicket"))
        } else if (parentTicket != null) {
            errors.add(ValidationUtils.invalidType("$.payload.ticket.parentTicket"))
        }

        val operationType = (ticket["operation"] as? JsonPrimitive)?.content
        if (operationType == "OPERATION_BUY_RETURN" || operationType == "OPERATION_SELL_RETURN") {
            if (parentTicket == null) {
                errors.add(ValidationUtils.missingField("$.payload.ticket.parentTicket"))
            }
        }

        val hasTicketTaxes = taxesElement is kotlinx.serialization.json.JsonArray && taxesElement.isNotEmpty()
        val hasItemTaxes = hasItemTaxes(ticket)
        if (hasTicketTaxes && hasItemTaxes) {
            errors.add(ValidationUtils.invalidValue("$.payload.ticket.taxes"))
        }

        val hasCashPayment = hasCashPayment(ticket)
        if (hasCashPayment) {
            val amounts = ticket["amounts"] as? JsonObject
            if (amounts == null) {
                errors.add(ValidationUtils.missingField("$.payload.ticket.amounts"))
            } else {
                if (amounts["taken"] == null) {
                    errors.add(ValidationUtils.missingField("$.payload.ticket.amounts.taken"))
                }
                if (amounts["change"] == null) {
                    errors.add(ValidationUtils.missingField("$.payload.ticket.amounts.change"))
                }
            }
        }

        val amounts = ticket["amounts"] as? JsonObject
        if (amounts != null) {
            if (amounts["markup"] != null && amounts["discount"] != null) {
                errors.add(ValidationUtils.invalidValue("$.payload.ticket.amounts"))
            }
        }

        return errors
    }

    /**
     * Проверяет наличие налогов в элементах списка товаров.
     */
    private fun hasItemTaxes(ticket: JsonObject): Boolean {
        val items = ticket["items"] as? kotlinx.serialization.json.JsonArray ?: return false
        return items.any { item ->
            val obj = item as? JsonObject ?: return@any false
            val commodityTaxes = (obj["commodity"] as? JsonObject)?.get(
                "taxes"
            ) as? kotlinx.serialization.json.JsonArray
            val stornoTaxes = (obj["stornoCommodity"] as? JsonObject)?.get(
                "taxes"
            ) as? kotlinx.serialization.json.JsonArray

            val hasTaxes = !commodityTaxes.isNullOrEmpty() || !stornoTaxes.isNullOrEmpty()
            hasTaxes
        }
    }

    /**
     * Проверяет наличие наличного платежа в списке платежей.
     */
    private fun hasCashPayment(ticket: JsonObject): Boolean {
        val payments = ticket["payments"] as? kotlinx.serialization.json.JsonArray ?: return false
        return payments.any { payment ->
            val obj = payment as? JsonObject ?: return@any false
            val type = obj["type"] as? JsonPrimitive ?: return@any false
            type.isString && type.content == "PAYMENT_CASH"
        }
    }
}
