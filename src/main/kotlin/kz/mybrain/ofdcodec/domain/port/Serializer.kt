package kz.mybrain.ofdcodec.domain.port

import kz.mybrain.ofdcodec.domain.model.CommandType
import kotlinx.serialization.json.JsonObject

/**
 * Сериализация JSON в бинарный payload согласно proto-библиотеке ОФД.
 */
interface Serializer {
    /**
     * Сериализует payload с учетом команды.
     */
    fun serialize(commandType: CommandType, json: JsonObject): ByteArray
}
