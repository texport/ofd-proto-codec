package kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.codec.closeshift

import kz.kazakhtelecom.proto.v203.Report
import kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.codec.common.DateTimeBuilder
import kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.codec.common.OperatorBuilder
import kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.codec.report.ZXReportBuilder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.longOrNull

/**
 * Сборщик CloseShiftRequest из JSON-структуры.
 */
class CloseShiftRequestBuilder {
    private val dateTimeBuilder = DateTimeBuilder()
    private val zxReportBuilder = ZXReportBuilder()
    private val operatorBuilder = OperatorBuilder()

    /**
     * Строит CloseShiftRequest из JSON-объекта payload.
     */
    fun build(payload: JsonObject): Report.CloseShiftRequest {
        val closeShiftJson = payload["closeShift"] as? JsonObject
            ?: throw IllegalArgumentException("Missing closeShift")

        val builder = Report.CloseShiftRequest.newBuilder()
        builder.setCloseTime(dateTimeBuilder.build(closeShiftJson, "closeTime"))

        readBool(closeShiftJson, "isOffline")?.let { builder.setIsOffline(it) }
        readUInt(closeShiftJson, "frShiftNumber")?.let { builder.setFrShiftNumber(it) }
        readBool(closeShiftJson, "withdrawMoney")?.let { builder.setWithdrawMoney(it) }
        readLong(closeShiftJson, "printedDocumentNumber")?.let { builder.setPrintedDocumentNumber(it) }

        val zxReportJson = closeShiftJson["zReport"] as? JsonObject
            ?: throw IllegalArgumentException("Missing zReport")
        builder.setZReport(zxReportBuilder.build(zxReportJson))

        val operatorJson = closeShiftJson["operator"] as? JsonObject
            ?: throw IllegalArgumentException("Missing operator")
        builder.setOperator(operatorBuilder.build(operatorJson))

        return builder.build()
    }

    /**
     * Читает boolean, если поле присутствует.
     */
    private fun readBool(json: JsonObject, key: String): Boolean? {
        val element = json[key] as? JsonPrimitive ?: return null
        return element.booleanOrNull
    }

    /**
     * Читает uint32, если поле присутствует.
     */
    private fun readUInt(json: JsonObject, key: String): Int? {
        val element = json[key] as? JsonPrimitive ?: return null
        val value = element.intOrNull ?: return null
        return if (value >= 0) value else null
    }

    /**
     * Читает uint64, если поле присутствует.
     */
    private fun readLong(json: JsonObject, key: String): Long? {
        val element = json[key] as? JsonPrimitive ?: return null
        val value = element.longOrNull ?: return null
        return if (value >= 0) value else null
    }
}
