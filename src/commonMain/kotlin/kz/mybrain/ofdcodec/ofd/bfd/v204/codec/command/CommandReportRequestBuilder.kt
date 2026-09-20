package kz.mybrain.ofdcodec.ofd.bfd.v204.codec.command

import kotlinx.serialization.json.JsonObject
import kz.bfd.proto.v204.CommandTypeEnum
import kz.bfd.proto.v204.Request
import kz.mybrain.ofdcodec.ofd.bfd.v204.codec.report.ReportRequestBuilder
import kz.mybrain.ofdcodec.ofd.bfd.v204.codec.service.ServiceRequestBuilder

/**
 * Сборщик Request для COMMAND_REPORT.
 */
internal class CommandReportRequestBuilder : CommandRequestBuilder {
    private val serviceRequestBuilder = ServiceRequestBuilder()
    private val reportRequestBuilder = ReportRequestBuilder()

    /**
     * Строит proto Request для COMMAND_REPORT на основе JSON payload.
     */
    override fun build(json: JsonObject): Request {
        val serviceRequest = serviceRequestBuilder.build(json)
        val reportRequest = reportRequestBuilder.build(json)
        return Request(
            command = CommandTypeEnum.COMMAND_REPORT,
            service = serviceRequest,
            report = reportRequest
        )
    }
}
