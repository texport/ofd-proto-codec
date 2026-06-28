package kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.codec.moneyplacement

import kz.kazakhtelecom.proto.v203.*

import kotlinx.serialization.json.JsonObject

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
internal class MoneyPlacementRequestBuilder {
    private val dateTimeBuilder = DateTimeBuilder()
    private val moneyBuilder = MoneyBuilder()
    private val operatorBuilder = OperatorBuilder()

    /**
     * Строит MoneyPlacementRequest из JSON-объекта payload.
     */
    fun build(payload: JsonObject): MoneyPlacementRequest {
        val placementJson = payload["moneyPlacement"] as? JsonObject
            ?: throw IllegalArgumentException("Missing moneyPlacement / Отсутствует moneyPlacement / moneyPlacement өрісі жетіспейді")

        val operatorJson = placementJson["operator"] as? JsonObject
            ?: throw IllegalArgumentException("Missing operator / Отсутствует operator / operator өрісі жетіспейді")
        val sumJson = placementJson["sum"] as? JsonObject
            ?: throw IllegalArgumentException("Missing sum / Отсутствует sum / sum өрісі жетіспейді")

        return MoneyPlacementRequest(
            datetime = dateTimeBuilder.build(placementJson, "dateTime"),
            operation = readOperationRequired(placementJson),
            sum = moneyBuilder.build(sumJson),
            is_offline = placementJson.readBool("isOffline"),
            fr_shift_number = placementJson.readInt("frShiftNumber"),
            printed_document_number = placementJson.readLong("printedDocumentNumber"),
            operator_ = operatorBuilder.build(operatorJson)
        )
    }

    /**
     * Читает enum MoneyPlacementEnum.
     */
    private fun readOperationRequired(json: JsonObject): MoneyPlacementEnum {
        val value = json.readStringRequired("operation")
        return MoneyPlacementEnum.valueOf(value)
    }
}
