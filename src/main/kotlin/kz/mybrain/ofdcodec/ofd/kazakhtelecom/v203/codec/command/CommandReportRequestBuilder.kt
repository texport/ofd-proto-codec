package kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.codec.command

import kotlinx.serialization.json.JsonObject
import kz.kazakhtelecom.proto.v203.Message
import kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.codec.report.ReportRequestBuilder
import kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.codec.service.ServiceRequestBuilder

/**
 * Сборщик Request для COMMAND_REPORT.
 */
internal class CommandReportRequestBuilder : CommandRequestBuilder {
    private val serviceRequestBuilder = ServiceRequestBuilder()
    private val reportRequestBuilder = ReportRequestBuilder()

    /**
     * Строит proto Request для COMMAND_REPORT на основе JSON payload.
     */
    override fun build(json: JsonObject): Message.Request {
        val serviceRequest = serviceRequestBuilder.build(json)
        val reportRequest = reportRequestBuilder.build(json)
        return Message.Request.newBuilder()
            .setCommand(Message.CommandTypeEnum.COMMAND_REPORT)
            .setService(serviceRequest)
            .setReport(reportRequest)
            .build()
    }
}
