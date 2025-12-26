package kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.codec.report

import kz.kazakhtelecom.proto.v203.Report
import kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.codec.common.DateTimeBuilder
import kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.codec.enums.ReportTypeBuilder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull

/**
 * Сборщик ReportRequest из JSON-структуры.
 */
class ReportRequestBuilder {
    private val dateTimeBuilder = DateTimeBuilder()
    private val reportTypeBuilder = ReportTypeBuilder()
    private val zxReportBuilder = ZXReportBuilder()

    /**
     * Строит ReportRequest из JSON-объекта payload.
     */
    fun build(payload: JsonObject): Report.ReportRequest {
        val reportJson = payload["report"] as? JsonObject
            ?: throw IllegalArgumentException("Missing report")

        val builder = Report.ReportRequest.newBuilder()
        builder.setReport(reportTypeBuilder.readRequired(reportJson, "reportType"))
        builder.setDateTime(dateTimeBuilder.build(reportJson, "dateTime"))

        readBool(reportJson, "isOffline")?.let { builder.setIsOffline(it) }

        val zxReportJson = reportJson["zxReport"] as? JsonObject
            ?: throw IllegalArgumentException("Missing zxReport")
        builder.setZxReport(zxReportBuilder.build(zxReportJson))

        return builder.build()
    }

    /**
     * Читает boolean, если поле присутствует.
     */
    private fun readBool(json: JsonObject, key: String): Boolean? {
        val element = json[key] as? JsonPrimitive ?: return null
        return element.booleanOrNull
    }
}
