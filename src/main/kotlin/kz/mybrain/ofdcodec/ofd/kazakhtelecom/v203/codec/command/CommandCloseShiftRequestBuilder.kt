package kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.codec.command

import kz.kazakhtelecom.proto.v203.Message
import kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.codec.closeshift.CloseShiftRequestBuilder
import kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.codec.service.ServiceRequestBuilder
import kotlinx.serialization.json.JsonObject

/**
 * Сборщик Request для COMMAND_CLOSE_SHIFT.
 */
class CommandCloseShiftRequestBuilder : CommandRequestBuilder {
    private val serviceRequestBuilder = ServiceRequestBuilder()
    private val closeShiftRequestBuilder = CloseShiftRequestBuilder()

    /**
     * Строит proto Request для COMMAND_CLOSE_SHIFT на основе JSON payload.
     */
    override fun build(json: JsonObject): Message.Request {
        val serviceRequest = serviceRequestBuilder.build(json)
        val closeShiftRequest = closeShiftRequestBuilder.build(json)
        return Message.Request.newBuilder()
            .setCommand(Message.CommandTypeEnum.COMMAND_CLOSE_SHIFT)
            .setService(serviceRequest)
            .setCloseShift(closeShiftRequest)
            .build()
    }
}
