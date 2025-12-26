package kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.validation

import kz.mybrain.ofdcodec.domain.model.CommandType
import kz.mybrain.ofdcodec.domain.model.ErrorCode
import kz.mybrain.ofdcodec.domain.model.ErrorFactory
import kz.mybrain.ofdcodec.domain.model.ValidationError
import kz.mybrain.ofdcodec.domain.port.Validator
import kotlinx.serialization.json.JsonObject

/**
 * Диспетчер валидаторов по типу команды.
 */
class CommandValidatorRegistry(
    private val validators: Map<CommandType, Validator>
) : Validator {
    /**
     * Проверяет JSON и собирает все ошибки по полям без остановки на первой.
     */
    override fun validate(commandType: CommandType, json: JsonObject): List<ValidationError> {
        val validator = validators[commandType]
        return if (validator == null) {
            listOf(
                ErrorFactory.error(
                    ErrorCode.COMMAND_UNSUPPORTED,
                    "$.commandType",
                    mapOf("command" to commandType.name)
                )
            )
        } else {
            validator.validate(commandType, json)
        }
    }
}
