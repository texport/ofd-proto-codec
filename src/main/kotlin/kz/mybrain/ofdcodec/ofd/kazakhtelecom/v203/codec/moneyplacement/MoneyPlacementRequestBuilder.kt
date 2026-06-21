package kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.codec.moneyplacement

import kotlinx.serialization.json.JsonObject
import kz.kazakhtelecom.proto.v203.Report
import kz.mybrain.ofdcodec.infrastructure.json.readBool
import kz.mybrain.ofdcodec.infrastructure.json.readInt
import kz.mybrain.ofdcodec.infrastructure.json.readLong
import kz.mybrain.ofdcodec.infrastructure.json.readStringRequired
import kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.codec.common.DateTimeBuilder
import kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.codec.common.MoneyBuilder
import kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.codec.common.OperatorBuilder

/**
 * Сборщик MoneyPlacementRequest из JSON-структуры.
 */
class MoneyPlacementRequestBuilder {
    private val dateTimeBuilder = DateTimeBuilder()
    private val moneyBuilder = MoneyBuilder()
    private val operatorBuilder = OperatorBuilder()

    /**
     * Строит MoneyPlacementRequest из JSON-объекта payload.
     */
    fun build(payload: JsonObject): Report.MoneyPlacementRequest {
        val placementJson = payload["moneyPlacement"] as? JsonObject
            ?: throw IllegalArgumentException("Missing moneyPlacement / Отсутствует moneyPlacement / moneyPlacement өрісі жетіспейді")

        val builder = Report.MoneyPlacementRequest.newBuilder()

        // Обязательная дата и время операции.
        builder.setDatetime(dateTimeBuilder.build(placementJson, "dateTime"))
        // Обязательный тип операции (внесение/снятие).
        builder.setOperation(readOperationRequired(placementJson))
        // Обязательная сумма операции.
        val sumJson = placementJson["sum"] as? JsonObject
            ?: throw IllegalArgumentException("Missing sum / Отсутствует sum / sum өрісі жетіспейді")
        builder.setSum(moneyBuilder.build(sumJson))

        // Опциональные поля.
        placementJson.readBool("isOffline")?.let { builder.setIsOffline(it) }
        placementJson.readInt("frShiftNumber")?.let { builder.setFrShiftNumber(it) }
        placementJson.readLong("printedDocumentNumber")?.let { builder.setPrintedDocumentNumber(it) }

        // Оператор обязателен для протокола 2.0.3.
        val operatorJson = placementJson["operator"] as? JsonObject
            ?: throw IllegalArgumentException("Missing operator / Отсутствует operator / operator өрісі жетіспейді")
        builder.setOperator(operatorBuilder.build(operatorJson))

        return builder.build()
    }

    /**
     * Читает enum MoneyPlacementEnum.
     */
    private fun readOperationRequired(json: JsonObject): Report.MoneyPlacementEnum {
        val value = json.readStringRequired("operation")
        return Report.MoneyPlacementEnum.valueOf(value)
    }
}
