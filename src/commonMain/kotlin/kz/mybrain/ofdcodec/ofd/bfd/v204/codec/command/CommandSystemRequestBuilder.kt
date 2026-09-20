package kz.mybrain.ofdcodec.ofd.bfd.v204.codec.command

import kotlinx.serialization.json.JsonObject
import kz.bfd.proto.v204.CommandTypeEnum
import kz.bfd.proto.v204.Request
import kz.mybrain.ofdcodec.ofd.bfd.v204.codec.service.ServiceRequestBuilder

/**
 * Построение запроса COMMAND_SYSTEM.
 * На первом этапе заполняем только базовые поля ServiceRequest.
 */
internal class CommandSystemRequestBuilder : CommandRequestBuilder {
    private val serviceRequestBuilder = ServiceRequestBuilder()

    /**
     * Собирает Request для команды COMMAND_SYSTEM.
     */
    override fun build(json: JsonObject): Request {
        val serviceRequest = serviceRequestBuilder.build(json)

        return Request(
            command = CommandTypeEnum.COMMAND_SYSTEM,
            service = serviceRequest
        )
    }
}
