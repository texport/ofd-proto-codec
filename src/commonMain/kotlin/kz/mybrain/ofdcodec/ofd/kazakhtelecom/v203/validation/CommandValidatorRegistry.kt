package kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.validation

import kotlinx.serialization.json.JsonObject
import kz.mybrain.ofdcodec.domain.model.CommandType
import kz.mybrain.ofdcodec.domain.model.ErrorCode
import kz.mybrain.ofdcodec.domain.model.ErrorFactory
import kz.mybrain.ofdcodec.domain.model.ValidationError
import kz.mybrain.ofdcodec.domain.port.Validator

/**
 * Диспетчер валидаторов по типу команды.
 */
internal class CommandValidatorRegistry(
    private val validators: Map<CommandType, Validator>
) : Validator {
    /**
     * Проверяет JSON и собирает все ошибки по полям без остановки на первой.
     */
    override fun validate(commandType: CommandType, json: JsonObject): List<ValidationError> {
        val validator = validators[commandType]
            ?: return listOf(
                ErrorFactory.error(
                    ErrorCode.COMMAND_UNSUPPORTED,
                    "$.commandType",
                    mapOf("command" to commandType.name)
                )
            )
        return validator.validate(commandType, json)
    }
}
