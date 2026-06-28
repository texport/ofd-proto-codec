package kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.codec.command

import kotlinx.serialization.json.JsonObject
import kz.kazakhtelecom.proto.v203.*
import kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.codec.service.ServiceRequestBuilder

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
