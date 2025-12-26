package kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.codec.moneyplacement

import kz.kazakhtelecom.proto.v203.Report
import kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.codec.common.DateTimeBuilder
import kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.codec.common.MoneyBuilder
import kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.codec.common.OperatorBuilder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.longOrNull

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
            ?: throw IllegalArgumentException("Missing moneyPlacement")

        val builder = Report.MoneyPlacementRequest.newBuilder()

        // Обязательная дата и время операции.
        builder.setDatetime(dateTimeBuilder.build(placementJson, "dateTime"))
        // Обязательный тип операции (внесение/снятие).
        builder.setOperation(readOperationRequired(placementJson, "operation"))
        // Обязательная сумма операции.
        val sumJson = placementJson["sum"] as? JsonObject ?: throw IllegalArgumentException("Missing sum")
        builder.setSum(moneyBuilder.build(sumJson))

        // Опциональные поля.
        readBool(placementJson, "isOffline")?.let { builder.setIsOffline(it) }
        readInt(placementJson, "frShiftNumber")?.let { builder.setFrShiftNumber(it) }
        readLong(placementJson, "printedDocumentNumber")?.let { builder.setPrintedDocumentNumber(it) }

        // Оператор обязателен для протокола 2.0.3.
        val operatorJson = placementJson["operator"] as? JsonObject
            ?: throw IllegalArgumentException("Missing operator")
        builder.setOperator(operatorBuilder.build(operatorJson))

        return builder.build()
    }

    /**
     * Читает enum MoneyPlacementEnum.
     */
    private fun readOperationRequired(json: JsonObject, key: String): Report.MoneyPlacementEnum {
        val value = readStringRequired(json, key)
        return Report.MoneyPlacementEnum.valueOf(value)
    }

    /**
     * Читает строку, если поле присутствует.
     */
    private fun readString(json: JsonObject, key: String): String? {
        val element = json[key] as? JsonPrimitive ?: return null
        return if (element.isString) element.content else null
    }

    /**
     * Читает обязательную строку или выбрасывает ошибку.
     */
    private fun readStringRequired(json: JsonObject, key: String): String {
        val value = readString(json, key)
        return value ?: throw IllegalArgumentException("Missing $key")
    }

    /**
     * Читает int, если поле присутствует.
     */
    private fun readInt(json: JsonObject, key: String): Int? {
        val element = json[key] as? JsonPrimitive ?: return null
        return element.intOrNull
    }

    /**
     * Читает обязательный int или выбрасывает ошибку.
     */
    private fun readIntRequired(json: JsonObject, key: String): Int {
        val value = readInt(json, key)
        return value ?: throw IllegalArgumentException("Missing $key")
    }

    /**
     * Читает long, если поле присутствует.
     */
    private fun readLong(json: JsonObject, key: String): Long? {
        val element = json[key] as? JsonPrimitive ?: return null
        return element.longOrNull
    }

    /**
     * Читает обязательный long или выбрасывает ошибку.
     */
    private fun readLongRequired(json: JsonObject, key: String): Long {
        val value = readLong(json, key)
        return value ?: throw IllegalArgumentException("Missing $key")
    }

    /**
     * Читает boolean, если поле присутствует.
     */
    private fun readBool(json: JsonObject, key: String): Boolean? {
        val element = json[key] as? JsonPrimitive ?: return null
        return element.booleanOrNull
    }
}
