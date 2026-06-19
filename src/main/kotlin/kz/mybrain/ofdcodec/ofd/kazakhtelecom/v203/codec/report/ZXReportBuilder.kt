package kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.codec.report

import kz.kazakhtelecom.proto.v203.Report
import kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.codec.common.DateTimeBuilder
import kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.codec.common.MoneyBuilder
import kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.codec.enums.OperationTypeBuilder
import kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.codec.enums.PaymentTypeBuilder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.intOrNull
import java.util.zip.CRC32

/**
 * Сборщик ZXReport из JSON-структуры.
 */
class ZXReportBuilder {
    private val dateTimeBuilder = DateTimeBuilder()
    private val moneyBuilder = MoneyBuilder()
    private val operationTypeBuilder = OperationTypeBuilder()
    private val paymentTypeBuilder = PaymentTypeBuilder()

    /**
     * Строит ZXReport из JSON-объекта.
     * Контрольная сумма вычисляется автоматически на основе данных отчета.
     */
    fun build(zxReportJson: JsonObject): Report.ZXReport {
        val builder = Report.ZXReport.newBuilder()

        // Обязательные поля: дата/время отчета и номер смены.
        builder.setDateTime(dateTimeBuilder.build(zxReportJson, "dateTime"))
        builder.setShiftNumber(readIntRequired(zxReportJson, "shiftNumber"))

        // Итоги по разделам (опционально).
        readArray(zxReportJson, "sections")?.forEach { builder.addSections(buildSection(it)) }
        // Итоги по операциям (опционально).
        readArray(zxReportJson, "operations")?.forEach { builder.addOperations(buildOperation(it)) }
        // Итоги по скидкам (опционально).
        readArray(zxReportJson, "discounts")?.forEach { builder.addDiscounts(buildOperation(it)) }
        // Итоги по наценкам (опционально).
        readArray(zxReportJson, "markups")?.forEach { builder.addMarkups(buildOperation(it)) }
        // Итоги по результатам (опционально).
        readArray(zxReportJson, "totalResult")?.forEach { builder.addTotalResult(buildOperation(it)) }

        // Налоги (опционально).
        readArray(zxReportJson, "taxes")?.forEach { builder.addTaxes(buildTax(it)) }

        // Необнуляемые суммы на начало смены (опционально).
        readArray(zxReportJson, "startShiftNonNullableSums")?.forEach {
            builder.addStartShiftNonNullableSums(buildNonNullableSum(it))
        }

        // Операции по чекам (опционально).
        readArray(zxReportJson, "ticketOperations")?.forEach { builder.addTicketOperations(buildTicketOperation(it)) }

        // Операции внесения/снятия (опционально).
        readArray(zxReportJson, "moneyPlacements")?.forEach { builder.addMoneyPlacements(buildMoneyPlacement(it)) }

        // Аннулированные чеки (опционально, deprecated).
        readObject(zxReportJson, "annulledTickets")?.let { builder.setAnnulledTickets(buildAnnulledTickets(it)) }

        // Обязательные поля: наличные в кассе и выручка.
        builder.setCashSum(moneyBuilder.build(readObjectRequired(zxReportJson, "cashSum")))
        builder.setRevenue(buildRevenue(readObjectRequired(zxReportJson, "revenue")))

        // Необнуляемые суммы на момент отчета (опционально).
        readArray(zxReportJson, "nonNullableSums")?.forEach { builder.addNonNullableSums(buildNonNullableSum(it)) }

        // Время открытия смены обязательно для v203.
        builder.setOpenShiftTime(dateTimeBuilder.build(zxReportJson, "openShiftTime"))
        // Время закрытия смены опционально для X-отчета и обязательно для Z-отчета (проверяется валидатором).
        readObject(zxReportJson, "closeShiftTime")?.let {
            builder.setCloseShiftTime(dateTimeBuilder.build(zxReportJson, "closeShiftTime"))
        }

        // Рассчитываем checksum по сериализованному отчету без поля checksum.
        val checksum = computeChecksum(builder.build().toByteArray())
        builder.setChecksum(checksum)

        return builder.build()
    }

    /**
     * Строит Section из JSON.
     */
    private fun buildSection(sectionJson: JsonObject): Report.ZXReport.Section {
        val builder = Report.ZXReport.Section.newBuilder()
        builder.setSectionCode(readStringRequired(sectionJson, "sectionCode"))
        val operations = readArray(sectionJson, "operations")
            ?: throw IllegalArgumentException("Missing operations")
        operations.forEach { builder.addOperations(buildOperation(it)) }
        return builder.build()
    }

    /**
     * Строит Operation из JSON.
     */
    private fun buildOperation(operationJson: JsonObject): Report.ZXReport.Operation {
        val builder = Report.ZXReport.Operation.newBuilder()
        builder.setOperation(operationTypeBuilder.readRequired(operationJson, "operation"))
        builder.setCount(readIntRequired(operationJson, "count"))
        builder.setSum(moneyBuilder.build(readObjectRequired(operationJson, "sum")))
        return builder.build()
    }

    /**
     * Строит Tax из JSON.
     */
    private fun buildTax(taxJson: JsonObject): Report.ZXReport.Tax {
        val builder = Report.ZXReport.Tax.newBuilder()
        builder.setTaxType(readIntRequired(taxJson, "taxType"))
        builder.setPercent(readIntRequired(taxJson, "percent"))
        val operations = readArray(taxJson, "operations")
            ?: throw IllegalArgumentException("Missing operations")
        operations.forEach { builder.addOperations(buildTaxOperation(it)) }
        return builder.build()
    }

    /**
     * Строит TaxOperation из JSON.
     */
    private fun buildTaxOperation(taxOperationJson: JsonObject): Report.ZXReport.Tax.TaxOperation {
        val builder = Report.ZXReport.Tax.TaxOperation.newBuilder()
        builder.setOperation(operationTypeBuilder.readRequired(taxOperationJson, "operation"))
        builder.setTurnover(moneyBuilder.build(readObjectRequired(taxOperationJson, "turnover")))
        builder.setSum(moneyBuilder.build(readObjectRequired(taxOperationJson, "sum")))
        builder.setTurnoverWithoutTax(moneyBuilder.build(readObjectRequired(taxOperationJson, "turnoverWithoutTax")))
        return builder.build()
    }

    /**
     * Строит NonNullableSum из JSON.
     */
    private fun buildNonNullableSum(sumJson: JsonObject): Report.ZXReport.NonNullableSum {
        val builder = Report.ZXReport.NonNullableSum.newBuilder()
        builder.setOperation(operationTypeBuilder.readRequired(sumJson, "operation"))
        builder.setSum(moneyBuilder.build(readObjectRequired(sumJson, "sum")))
        return builder.build()
    }

    /**
     * Строит TicketOperation из JSON.
     */
    private fun buildTicketOperation(ticketJson: JsonObject): Report.ZXReport.TicketOperation {
        val builder = Report.ZXReport.TicketOperation.newBuilder()
        builder.setOperation(operationTypeBuilder.readRequired(ticketJson, "operation"))
        builder.setTicketsTotalCount(readIntRequired(ticketJson, "ticketsTotalCount"))
        builder.setTicketsCount(readIntRequired(ticketJson, "ticketsCount"))
        builder.setTicketsSum(moneyBuilder.build(readObjectRequired(ticketJson, "ticketsSum")))
        val payments = readArray(ticketJson, "payments")
            ?: throw IllegalArgumentException("Missing payments")
        payments.forEach { builder.addPayments(buildTicketPayment(it)) }
        builder.setOfflineCount(readIntRequired(ticketJson, "offlineCount"))
        builder.setDiscountSum(moneyBuilder.build(readObjectRequired(ticketJson, "discountSum")))
        builder.setMarkupSum(moneyBuilder.build(readObjectRequired(ticketJson, "markupSum")))
        builder.setChangeSum(moneyBuilder.build(readObjectRequired(ticketJson, "changeSum")))
        return builder.build()
    }

    /**
     * Строит TicketOperation.Payment из JSON.
     */
    private fun buildTicketPayment(paymentJson: JsonObject): Report.ZXReport.TicketOperation.Payment {
        val builder = Report.ZXReport.TicketOperation.Payment.newBuilder()
        builder.setPayment(paymentTypeBuilder.readRequired(paymentJson, "payment"))
        builder.setSum(moneyBuilder.build(readObjectRequired(paymentJson, "sum")))
        builder.setCount(readIntRequired(paymentJson, "count"))
        return builder.build()
    }

    /**
     * Строит MoneyPlacement из JSON.
     */
    private fun buildMoneyPlacement(placementJson: JsonObject): Report.ZXReport.MoneyPlacement {
        val builder = Report.ZXReport.MoneyPlacement.newBuilder()
        val op = readStringRequired(placementJson, "operation")
        builder.setOperation(Report.MoneyPlacementEnum.valueOf(op))
        builder.setOperationsTotalCount(readIntRequired(placementJson, "operationsTotalCount"))
        builder.setOperationsCount(readIntRequired(placementJson, "operationsCount"))
        builder.setOperationsSum(moneyBuilder.build(readObjectRequired(placementJson, "operationsSum")))
        builder.setOfflineCount(readIntRequired(placementJson, "offlineCount"))
        return builder.build()
    }

    /**
     * Строит AnnulledTickets из JSON (deprecated).
     */
    private fun buildAnnulledTickets(annulledJson: JsonObject): Report.ZXReport.AnnulledTickets {
        val builder = Report.ZXReport.AnnulledTickets.newBuilder()
        builder.setAnnulledTicketsTotalCount(readIntRequired(annulledJson, "annulledTicketsTotalCount"))
        builder.setAnnulledTicketsCount(readIntRequired(annulledJson, "annulledTicketsCount"))
        readArray(annulledJson, "annulledOperations")?.forEach { builder.addAnnulledOperations(buildOperation(it)) }
        return builder.build()
    }

    /**
     * Строит Revenue из JSON.
     */
    private fun buildRevenue(revenueJson: JsonObject): Report.ZXReport.Revenue {
        val builder = Report.ZXReport.Revenue.newBuilder()
        builder.setSum(moneyBuilder.build(readObjectRequired(revenueJson, "sum")))
        builder.setIsNegative(readBooleanRequired(revenueJson, "isNegative"))
        return builder.build()
    }

    /**
     * Вычисляет CRC32 и возвращает hex-строку.
     */
    private fun computeChecksum(bytes: ByteArray): String {
        val crc32 = CRC32()
        crc32.update(bytes)
        return crc32.value.toString(16).padStart(8, '0').uppercase()
    }

    private fun readObject(json: JsonObject, key: String): JsonObject? = json[key] as? JsonObject

    private fun readObjectRequired(json: JsonObject, key: String): JsonObject {
        return readObject(json, key) ?: throw IllegalArgumentException("Missing $key")
    }

    private fun readArray(json: JsonObject, key: String): List<JsonObject>? {
        val array = json[key] as? JsonArray ?: return null
        return array.mapNotNull { it as? JsonObject }
    }

    private fun readStringRequired(json: JsonObject, key: String): String {
        val element = json[key] as? JsonPrimitive
        require(element != null && element.isString) { "Missing $key" }
        return element.content
    }

    private fun readInt(json: JsonObject, key: String): Int? {
        val element = json[key] as? JsonPrimitive ?: return null
        return element.intOrNull
    }

    private fun readIntRequired(json: JsonObject, key: String): Int {
        return readInt(json, key) ?: throw IllegalArgumentException("Missing $key")
    }

    private fun readBooleanRequired(json: JsonObject, key: String): Boolean {
        val element = json[key] as? JsonPrimitive
        return element?.booleanOrNull ?: throw IllegalArgumentException("Missing $key")
    }
}
