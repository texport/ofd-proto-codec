package kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.validation.response

import kz.mybrain.ofdcodec.domain.model.CommandType
import kz.mybrain.ofdcodec.domain.model.ValidationError
import kz.mybrain.ofdcodec.domain.port.Validator
import kotlinx.serialization.json.JsonObject

/**
 * Валидатор ответа для COMMAND_AUTH.
 */
class ResponseValidatorAuth : Validator {
    /**
     * Проверяет JSON и собирает все ошибки по полям без остановки на первой.
     * Пока проверок нет, возвращается пустой список ошибок.
     */
    override fun validate(commandType: CommandType, json: JsonObject): List<ValidationError> = emptyList()
}
