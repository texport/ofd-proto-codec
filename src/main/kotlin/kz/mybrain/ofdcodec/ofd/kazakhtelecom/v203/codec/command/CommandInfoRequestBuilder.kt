package kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.codec.command

import kz.kazakhtelecom.proto.v203.Message
import kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.codec.service.ServiceRequestBuilder
import kotlinx.serialization.json.JsonObject

/**
 * Построение запроса COMMAND_INFO.
 * Использует ServiceRequest с обязательными служебными данными.
 */
class CommandInfoRequestBuilder : CommandRequestBuilder {
    private val serviceRequestBuilder = ServiceRequestBuilder()

    /**
     * Собирает Message.Request для команды COMMAND_INFO.
     */
    override fun build(json: JsonObject): Message.Request {
        val serviceRequest = serviceRequestBuilder.build(json)

        return Message.Request.newBuilder()
            .setCommand(Message.CommandTypeEnum.COMMAND_INFO)
            .setService(serviceRequest)
            .build()
    }
}
