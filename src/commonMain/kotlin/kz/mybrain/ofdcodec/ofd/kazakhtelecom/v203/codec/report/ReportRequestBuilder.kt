package kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.codec.report

import kotlinx.serialization.json.JsonObject
import kz.kazakhtelecom.proto.v203.*
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

    fun build(payload: JsonObject): ReportRequest {
        val reportJson = payload["report"] as? JsonObject
            ?: throw IllegalArgumentException("Missing report / Отсутствует report / report өрісі жетіспейді")

        val zxReportJson = reportJson["zxReport"] as? JsonObject
            ?: throw IllegalArgumentException("Missing zxReport / Отсутствует zxReport / zxReport өрісі жетіспейді")

        return ReportRequest(
            report = reportTypeBuilder.readRequired(reportJson, "reportType"),
            date_time = dateTimeBuilder.build(reportJson, "dateTime"),
            is_offline = reportJson.readBool("isOffline"),
            zx_report = zxReportBuilder.build(zxReportJson)
        )
    }
}
