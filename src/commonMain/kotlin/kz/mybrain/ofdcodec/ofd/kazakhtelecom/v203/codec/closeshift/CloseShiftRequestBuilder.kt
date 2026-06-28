package kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.codec.closeshift

import kotlinx.serialization.json.JsonObject
import kz.kazakhtelecom.proto.v203.*
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

    fun build(payload: JsonObject): CloseShiftRequest {
        val closeShiftJson = payload["closeShift"] as? JsonObject
            ?: throw IllegalArgumentException("Missing closeShift / Отсутствует closeShift / closeShift өрісі жетіспейді")

        val zxReportJson = closeShiftJson["zReport"]
        require(zxReportJson is JsonObject) { "Missing zReport / Отсутствует zReport / zReport өрісі жетіспейді" }

        val operatorJson = closeShiftJson["operator"]
        require(operatorJson is JsonObject) { "Missing operator / Отсутствует operator / operator өрісі жетіспейді" }

        return CloseShiftRequest(
            close_time = dateTimeBuilder.build(closeShiftJson, "closeTime"),
            is_offline = closeShiftJson.readBool("isOffline"),
            fr_shift_number = closeShiftJson.readInt("frShiftNumber"),
            withdraw_money = closeShiftJson.readBool("withdrawMoney"),
            printed_document_number = closeShiftJson.readLong("printedDocumentNumber"),
            z_report = zxReportBuilder.build(zxReportJson),
            operator_ = operatorBuilder.build(operatorJson)
        )
    }
}
