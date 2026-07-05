package kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.codec

import kotlinx.serialization.json.JsonObject
import kz.kazakhtelecom.proto.v203.Request
import kz.mybrain.ofdcodec.domain.model.CommandType
import kz.mybrain.ofdcodec.domain.port.Serializer
import kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.codec.command.CommandRequestBuilder

/**
 * Сериализация запросов ККМ для текущего provider module 2.0.3.
 */
internal class KazakhtelecomV203RequestSerializer(
    private val builders: Map<CommandType, CommandRequestBuilder>
) : Serializer {
    /**
     * Сериализует payload в proto Request по типу команды.
     */
    override fun serialize(commandType: CommandType, json: JsonObject): ByteArray {
        val builder = builders[commandType]
            ?: throw IllegalArgumentException(
                "Unsupported command: ${commandType.name} / " +
                    "Неподдерживаемая команда: ${commandType.name} / " +
                    "${commandType.name} пәрменіне қолдау көрсетілмейді"
            )
        return Request.ADAPTER.encode(builder.build(json))
    }
}
