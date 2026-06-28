package kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.validation.response

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull
import kz.mybrain.ofdcodec.domain.model.CommandType
import kz.mybrain.ofdcodec.domain.model.ValidationError
import kz.mybrain.ofdcodec.domain.port.Validator
import kz.mybrain.ofdcodec.domain.validation.ValidationUtils
import kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.validation.response.service.ServiceResponseValidator

/**
 * Валидатор ответа для COMMAND_TICKET.
 *
 * Требует result.resultCode, а при успешном результате — блок ticket
 * с номером чека и опциональным QR.
 */
internal class ResponseValidatorTicket : Validator {
    private val serviceValidator = ServiceResponseValidator()

    /**
     * Проверяет JSON ответа по COMMAND_TICKET и собирает все ошибки без раннего выхода.
     */
    override fun validate(commandType: CommandType, json: JsonObject): List<ValidationError> {
        val errors = mutableListOf<ValidationError>()

        // Проверяем блок result и корректность кода результата.
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

        val resultCode = (result["resultCode"] as? JsonPrimitive)?.intOrNull

        // Проверяем блок ticket: обязателен при успешной операции.
        val ticket = json["ticket"]
        if (resultCode == 0) {
            val ticketObject = ticket as? JsonObject
            if (ticketObject == null) {
                errors.add(ValidationUtils.missingField("$.payload.ticket"))
            } else {
                ValidationUtils.requireNonBlankString(
                    ticketObject,
                    "ticketNumber",
                    "$.payload.ticket.ticketNumber",
                    errors
                )
                ValidationUtils.optionalNonBlankString(
                    ticketObject,
                    "qrCodeBase64",
                    "$.payload.ticket.qrCodeBase64",
                    errors
                )
            }
        } else if (ticket != null) {
            val ticketObject = ticket as? JsonObject
            if (ticketObject == null) {
                errors.add(ValidationUtils.invalidType("$.payload.ticket"))
            } else {
                ValidationUtils.optionalNonBlankString(
                    ticketObject,
                    "ticketNumber",
                    "$.payload.ticket.ticketNumber",
                    errors
                )
                ValidationUtils.optionalNonBlankString(
                    ticketObject,
                    "qrCodeBase64",
                    "$.payload.ticket.qrCodeBase64",
                    errors
                )
            }
        }

        // Служебная часть опциональна, но если пришла — валидируем по протоколу.
        val service = json["service"]
        if (service != null) {
            val serviceObject = service as? JsonObject
            if (serviceObject == null) {
                errors.add(ValidationUtils.invalidType("$.payload.service"))
            } else {
                errors.addAll(serviceValidator.validate(serviceObject, "$.payload.service"))
            }
        }

        return errors
    }
}
