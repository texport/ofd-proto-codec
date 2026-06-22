package kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.codec.report

import kotlinx.serialization.json.JsonObject
import kz.kazakhtelecom.proto.v203.Report
import kz.mybrain.ofdcodec.infrastructure.json.readBoolRequired
import kz.mybrain.ofdcodec.infrastructure.json.readIntRequired
import kz.mybrain.ofdcodec.infrastructure.json.readObject
import kz.mybrain.ofdcodec.infrastructure.json.readObjectList
import kz.mybrain.ofdcodec.infrastructure.json.readObjectRequired
import kz.mybrain.ofdcodec.infrastructure.json.readStringRequired
import kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.codec.common.DateTimeBuilder
import kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.codec.common.MoneyBuilder
import kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.codec.enums.OperationTypeBuilder
import kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.codec.enums.PaymentTypeBuilder
import java.util.zip.CRC32

/**
 * Сборщик ZXReport из JSON-структуры.
 */
internal class ZXReportBuilder {
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
        builder.setShiftNumber(zxReportJson.readIntRequired("shiftNumber"))

        // Итоги по разделам (опционально).
        zxReportJson.readObjectList("sections")?.forEach { builder.addSections(buildSection(it)) }
        // Итоги по операциям (опционально).
        zxReportJson.readObjectList("operations")?.forEach { builder.addOperations(buildOperation(it)) }
        // Итоги по скидкам (опционально).
        zxReportJson.readObjectList("discounts")?.forEach { builder.addDiscounts(buildOperation(it)) }
        // Итоги по наценкам (опционально).
        zxReportJson.readObjectList("markups")?.forEach { builder.addMarkups(buildOperation(it)) }
        // Итоги по результатам (опционально).
        zxReportJson.readObjectList("totalResult")?.forEach { builder.addTotalResult(buildOperation(it)) }

        // Налоги (опционально).
        zxReportJson.readObjectList("taxes")?.forEach { builder.addTaxes(buildTax(it)) }

        // Необнуляемые суммы на начало смены (опционально).
        zxReportJson.readObjectList("startShiftNonNullableSums")?.forEach {
            builder.addStartShiftNonNullableSums(buildNonNullableSum(it))
        }

        // Операции по чекам (опционально).
        zxReportJson.readObjectList(
            "ticketOperations"
        )?.forEach { builder.addTicketOperations(buildTicketOperation(it)) }

        // Операции внесения/снятия (опционально).
        zxReportJson.readObjectList("moneyPlacements")?.forEach { builder.addMoneyPlacements(buildMoneyPlacement(it)) }

        // Аннулированные чеки (опционально, deprecated).
        zxReportJson.readObject("annulledTickets")?.let { builder.setAnnulledTickets(buildAnnulledTickets(it)) }

        // Обязательные поля: наличные в кассе и выручка.
        builder.setCashSum(moneyBuilder.build(zxReportJson.readObjectRequired("cashSum")))
        builder.setRevenue(buildRevenue(zxReportJson.readObjectRequired("revenue")))

        // Необнуляемые суммы на момент отчета (опционально).
        zxReportJson.readObjectList("nonNullableSums")?.forEach { builder.addNonNullableSums(buildNonNullableSum(it)) }

        // Время открытия смены обязательно для v203.
        builder.setOpenShiftTime(dateTimeBuilder.build(zxReportJson, "openShiftTime"))
        // Время закрытия смены опционально для X-отчета и обязательно для Z-отчета (проверяется валидатором).
        zxReportJson.readObject("closeShiftTime")?.let {
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
        builder.setSectionCode(sectionJson.readStringRequired("sectionCode"))
        val operations = sectionJson.readObjectList("operations")
            ?: throw IllegalArgumentException("Missing operations / Отсутствует operations / operations өрісі жетіспейді")
        operations.forEach { builder.addOperations(buildOperation(it)) }
        return builder.build()
    }

    /**
     * Строит Operation из JSON.
     */
    private fun buildOperation(operationJson: JsonObject): Report.ZXReport.Operation {
        val builder = Report.ZXReport.Operation.newBuilder()
        builder.setOperation(operationTypeBuilder.readRequired(operationJson, "operation"))
        builder.setCount(operationJson.readIntRequired("count"))
        builder.setSum(moneyBuilder.build(operationJson.readObjectRequired("sum")))
        return builder.build()
    }

    /**
     * Строит Tax из JSON.
     */
    private fun buildTax(taxJson: JsonObject): Report.ZXReport.Tax {
        val builder = Report.ZXReport.Tax.newBuilder()
        builder.setTaxType(taxJson.readIntRequired("taxType"))
        builder.setPercent(taxJson.readIntRequired("percent"))
        val operations = taxJson.readObjectList("operations")
            ?: throw IllegalArgumentException("Missing operations / Отсутствует operations / operations өрісі жетіспейді")
        operations.forEach { builder.addOperations(buildTaxOperation(it)) }
        return builder.build()
    }

    /**
     * Строит TaxOperation из JSON.
     */
    private fun buildTaxOperation(taxOperationJson: JsonObject): Report.ZXReport.Tax.TaxOperation {
        val builder = Report.ZXReport.Tax.TaxOperation.newBuilder()
        builder.setOperation(operationTypeBuilder.readRequired(taxOperationJson, "operation"))
        builder.setTurnover(moneyBuilder.build(taxOperationJson.readObjectRequired("turnover")))
        builder.setSum(moneyBuilder.build(taxOperationJson.readObjectRequired("sum")))
        builder.setTurnoverWithoutTax(moneyBuilder.build(taxOperationJson.readObjectRequired("turnoverWithoutTax")))
        return builder.build()
    }

    /**
     * Строит NonNullableSum из JSON.
     */
    private fun buildNonNullableSum(sumJson: JsonObject): Report.ZXReport.NonNullableSum {
        val builder = Report.ZXReport.NonNullableSum.newBuilder()
        builder.setOperation(operationTypeBuilder.readRequired(sumJson, "operation"))
        builder.setSum(moneyBuilder.build(sumJson.readObjectRequired("sum")))
        return builder.build()
    }

    /**
     * Строит TicketOperation из JSON.
     */
    private fun buildTicketOperation(ticketJson: JsonObject): Report.ZXReport.TicketOperation {
        val builder = Report.ZXReport.TicketOperation.newBuilder()
        builder.setOperation(operationTypeBuilder.readRequired(ticketJson, "operation"))
        builder.setTicketsTotalCount(ticketJson.readIntRequired("ticketsTotalCount"))
        builder.setTicketsCount(ticketJson.readIntRequired("ticketsCount"))
        builder.setTicketsSum(moneyBuilder.build(ticketJson.readObjectRequired("ticketsSum")))
        val payments = ticketJson.readObjectList("payments")
            ?: throw IllegalArgumentException("Missing payments / Отсутствует payments / payments өрісі жетіспейді")
        payments.forEach { builder.addPayments(buildTicketPayment(it)) }
        builder.setOfflineCount(ticketJson.readIntRequired("offlineCount"))
        builder.setDiscountSum(moneyBuilder.build(ticketJson.readObjectRequired("discountSum")))
        builder.setMarkupSum(moneyBuilder.build(ticketJson.readObjectRequired("markupSum")))
        builder.setChangeSum(moneyBuilder.build(ticketJson.readObjectRequired("changeSum")))
        return builder.build()
    }

    /**
     * Строит TicketOperation.Payment из JSON.
     */
    private fun buildTicketPayment(paymentJson: JsonObject): Report.ZXReport.TicketOperation.Payment {
        val builder = Report.ZXReport.TicketOperation.Payment.newBuilder()
        builder.setPayment(paymentTypeBuilder.readRequired(paymentJson, "payment"))
        builder.setSum(moneyBuilder.build(paymentJson.readObjectRequired("sum")))
        builder.setCount(paymentJson.readIntRequired("count"))
        return builder.build()
    }

    /**
     * Строит MoneyPlacement из JSON.
     */
    private fun buildMoneyPlacement(placementJson: JsonObject): Report.ZXReport.MoneyPlacement {
        val builder = Report.ZXReport.MoneyPlacement.newBuilder()
        val op = placementJson.readStringRequired("operation")
        builder.setOperation(Report.MoneyPlacementEnum.valueOf(op))
        builder.setOperationsTotalCount(placementJson.readIntRequired("operationsTotalCount"))
        builder.setOperationsCount(placementJson.readIntRequired("operationsCount"))
        builder.setOperationsSum(moneyBuilder.build(placementJson.readObjectRequired("operationsSum")))
        builder.setOfflineCount(placementJson.readIntRequired("offlineCount"))
        return builder.build()
    }

    /**
     * Строит AnnulledTickets из JSON (deprecated).
     */
    private fun buildAnnulledTickets(annulledJson: JsonObject): Report.ZXReport.AnnulledTickets {
        val builder = Report.ZXReport.AnnulledTickets.newBuilder()
        builder.setAnnulledTicketsTotalCount(annulledJson.readIntRequired("annulledTicketsTotalCount"))
        builder.setAnnulledTicketsCount(annulledJson.readIntRequired("annulledTicketsCount"))
        annulledJson.readObjectList("annulledOperations")?.forEach { builder.addAnnulledOperations(buildOperation(it)) }
        return builder.build()
    }

    /**
     * Строит Revenue из JSON.
     */
    private fun buildRevenue(revenueJson: JsonObject): Report.ZXReport.Revenue {
        val builder = Report.ZXReport.Revenue.newBuilder()
        builder.setSum(moneyBuilder.build(revenueJson.readObjectRequired("sum")))
        builder.setIsNegative(revenueJson.readBoolRequired("isNegative"))
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
}
