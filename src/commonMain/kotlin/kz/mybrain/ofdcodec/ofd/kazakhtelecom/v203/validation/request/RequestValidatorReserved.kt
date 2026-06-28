package kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.validation.request

import kotlinx.serialization.json.JsonObject
import kz.mybrain.ofdcodec.domain.model.CommandType
import kz.mybrain.ofdcodec.domain.model.ValidationError
import kz.mybrain.ofdcodec.domain.port.Validator

/**
 * Валидатор запроса для COMMAND_RESERVED.
 */
internal class RequestValidatorReserved : Validator {
    /**
     * Проверяет JSON и собирает все ошибки по полям без остановки на первой.
     * Пока проверок нет, возвращается пустой список ошибок.
     */
    override fun validate(commandType: CommandType, json: JsonObject): List<ValidationError> = emptyList()
}
