package kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.codec.command

import kz.kazakhtelecom.proto.v203.*
import kotlinx.serialization.json.JsonObject
import kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.codec.service.ServiceRequestBuilder

/**
 * Построение запроса COMMAND_INFO.
 * Использует ServiceRequest с обязательными служебными данными.
 */
internal class CommandInfoRequestBuilder : CommandRequestBuilder {
    private val serviceRequestBuilder = ServiceRequestBuilder()

    /**
     * Собирает Request для команды COMMAND_INFO.
     */
    override fun build(json: JsonObject): Request {
        val serviceRequest = serviceRequestBuilder.build(json)

        return Request(
            command = CommandTypeEnum.COMMAND_INFO,
            service = serviceRequest
        )
    }
}
