package kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.codec.report

import kotlinx.serialization.json.JsonObject
import kz.kazakhtelecom.proto.v203.Report
import kz.mybrain.ofdcodec.infrastructure.json.readBool
import kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.codec.common.DateTimeBuilder
import kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.codec.enums.ReportTypeBuilder

/**
 * Сборщик ReportRequest из JSON-структуры.
 */
internal class ReportRequestBuilder {
    private val dateTimeBuilder = DateTimeBuilder()
    private val reportTypeBuilder = ReportTypeBuilder()
    private val zxReportBuilder = ZXReportBuilder()

    /**
     * Строит ReportRequest из JSON-объекта payload.
     */
    fun build(payload: JsonObject): Report.ReportRequest {
        val reportJson = payload["report"] as? JsonObject
            ?: throw IllegalArgumentException("Missing report / Отсутствует report / report өрісі жетіспейді")

        val builder = Report.ReportRequest.newBuilder()
        builder.setReport(reportTypeBuilder.readRequired(reportJson, "reportType"))
        builder.setDateTime(dateTimeBuilder.build(reportJson, "dateTime"))

        reportJson.readBool("isOffline")?.let { builder.setIsOffline(it) }

        val zxReportJson = reportJson["zxReport"] as? JsonObject
            ?: throw IllegalArgumentException("Missing zxReport / Отсутствует zxReport / zxReport өрісі жетіспейді")
        builder.setZxReport(zxReportBuilder.build(zxReportJson))

        return builder.build()
    }
}
