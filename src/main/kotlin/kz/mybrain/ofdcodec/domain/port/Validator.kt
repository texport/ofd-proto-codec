package kz.mybrain.ofdcodec.domain.port

import kotlinx.serialization.json.JsonObject
import kz.mybrain.ofdcodec.domain.model.CommandType
import kz.mybrain.ofdcodec.domain.model.ValidationError

/**
 * Валидатор бизнес-логики для JSON-представления сообщений.
 */
interface Validator {
    /**
     * Валидирует payload с учетом команды.
     */
    fun validate(commandType: CommandType, json: JsonObject): List<ValidationError>
}
