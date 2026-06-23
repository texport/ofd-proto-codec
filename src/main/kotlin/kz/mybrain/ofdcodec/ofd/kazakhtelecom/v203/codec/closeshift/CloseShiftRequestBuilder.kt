package kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.codec.closeshift

import kotlinx.serialization.json.JsonObject
import kz.kazakhtelecom.proto.v203.Report
import kz.mybrain.ofdcodec.infrastructure.json.readBool
import kz.mybrain.ofdcodec.infrastructure.json.readInt
import kz.mybrain.ofdcodec.infrastructure.json.readLong
import kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.codec.common.DateTimeBuilder
import kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.codec.common.OperatorBuilder
import kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.codec.report.ZXReportBuilder

/**
 * Сборщик CloseShiftRequest из JSON-структуры.
 */
internal class CloseShiftRequestBuilder {
    private val dateTimeBuilder = DateTimeBuilder()
    private val zxReportBuilder = ZXReportBuilder()
    private val operatorBuilder = OperatorBuilder()

    /**
     * Строит CloseShiftRequest из JSON-объекта payload.
     */
    fun build(payload: JsonObject): Report.CloseShiftRequest {
        val closeShiftJson = payload["closeShift"] as? JsonObject
            ?: throw IllegalArgumentException("Missing closeShift / Отсутствует closeShift / closeShift өрісі жетіспейді")

        val builder = Report.CloseShiftRequest.newBuilder()
        builder.setCloseTime(dateTimeBuilder.build(closeShiftJson, "closeTime"))

        closeShiftJson.readBool("isOffline")?.let { builder.setIsOffline(it) }
        closeShiftJson.readInt("frShiftNumber")?.let { builder.setFrShiftNumber(it) }
        closeShiftJson.readBool("withdrawMoney")?.let { builder.setWithdrawMoney(it) }
        closeShiftJson.readLong("printedDocumentNumber")?.let { builder.setPrintedDocumentNumber(it) }

        val zxReportJson = closeShiftJson["zReport"]
        require(zxReportJson is JsonObject) { "Missing zReport / Отсутствует zReport / zReport өрісі жетіспейді" }
        builder.setZReport(zxReportBuilder.build(zxReportJson))

        val operatorJson = closeShiftJson["operator"]
        require(operatorJson is JsonObject) { "Missing operator / Отсутствует operator / operator өрісі жетіспейді" }
        builder.setOperator(operatorBuilder.build(operatorJson))

        return builder.build()
    }
}
