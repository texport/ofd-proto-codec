package kz.mybrain.ofdcodec.ofd.bfd.v204.codec.command

import kotlinx.serialization.json.JsonObject
import kz.bfd.proto.v204.CommandTypeEnum
import kz.bfd.proto.v204.Request
import kz.mybrain.ofdcodec.ofd.bfd.v204.codec.closeshift.CloseShiftRequestBuilder
import kz.mybrain.ofdcodec.ofd.bfd.v204.codec.service.ServiceRequestBuilder

/**
 * Сборщик Request для COMMAND_CLOSE_SHIFT.
 */
internal class CommandCloseShiftRequestBuilder : CommandRequestBuilder {
    private val serviceRequestBuilder = ServiceRequestBuilder()
    private val closeShiftRequestBuilder = CloseShiftRequestBuilder()

    /**
     * Строит proto Request для COMMAND_CLOSE_SHIFT на основе JSON payload.
     */
    override fun build(json: JsonObject): Request {
        val serviceRequest = serviceRequestBuilder.build(json)
        val closeShiftRequest = closeShiftRequestBuilder.build(json)
        return Request(
            command = CommandTypeEnum.COMMAND_CLOSE_SHIFT,
            service = serviceRequest,
            close_shift = closeShiftRequest
        )
    }
}
