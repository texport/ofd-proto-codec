package kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.codec

import kz.mybrain.ofdcodec.domain.model.CommandType
import kz.mybrain.ofdcodec.domain.port.Serializer
import kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.codec.command.CommandRequestBuilder
import kotlinx.serialization.json.JsonObject

/**
 * Сериализация запросов ККМ для протокола Казахтелеком 2.0.3.
 */
class KazakhtelecomV203RequestSerializer(
    private val builders: Map<CommandType, CommandRequestBuilder>
) : Serializer {
    /**
     * Сериализует payload в proto Request по типу команды.
     */
    override fun serialize(commandType: CommandType, json: JsonObject): ByteArray {
        val builder = builders[commandType]
            ?: throw IllegalArgumentException("Unsupported command: ${commandType.name}")
        return builder.build(json).toByteArray()
    }
}
