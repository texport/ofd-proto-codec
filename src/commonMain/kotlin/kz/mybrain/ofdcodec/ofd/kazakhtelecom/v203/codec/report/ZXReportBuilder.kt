package kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.codec.report

import kz.kazakhtelecom.proto.v203.*

import kotlinx.serialization.json.JsonObject
import kz.mybrain.ofdcodec.infrastructure.json.readBool
import kz.mybrain.ofdcodec.infrastructure.json.readInt
import kz.mybrain.ofdcodec.infrastructure.json.readLong
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
import kz.mybrain.ofdcodec.infrastructure.util.Crc32

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
    fun build(zxReportJson: JsonObject): ZXReport {
        val dateTime = dateTimeBuilder.build(zxReportJson, "dateTime")
        val shiftNumber = zxReportJson.readIntRequired("shiftNumber")

        val sections = zxReportJson.readObjectList("sections")?.map { buildSection(it) } ?: emptyList()
        val operations = zxReportJson.readObjectList("operations")?.map { buildOperation(it) } ?: emptyList()
        val discounts = zxReportJson.readObjectList("discounts")?.map { buildOperation(it) } ?: emptyList()
        val markups = zxReportJson.readObjectList("markups")?.map { buildOperation(it) } ?: emptyList()
        val totalResult = zxReportJson.readObjectList("totalResult")?.map { buildOperation(it) } ?: emptyList()
        val taxes = zxReportJson.readObjectList("taxes")?.map { buildTax(it) } ?: emptyList()
        val startShiftNonNullableSums = zxReportJson.readObjectList("startShiftNonNullableSums")?.map { buildNonNullableSum(it) } ?: emptyList()
        val ticketOperations = zxReportJson.readObjectList("ticketOperations")?.map { buildTicketOperation(it) } ?: emptyList()
        val moneyPlacements = zxReportJson.readObjectList("moneyPlacements")?.map { buildMoneyPlacement(it) } ?: emptyList()
        val annulledTickets = zxReportJson.readObject("annulledTickets")?.let { buildAnnulledTickets(it) }
        val cashSum = moneyBuilder.build(zxReportJson.readObjectRequired("cashSum"))
        val revenue = buildRevenue(zxReportJson.readObjectRequired("revenue"))
        val nonNullableSums = zxReportJson.readObjectList("nonNullableSums")?.map { buildNonNullableSum(it) } ?: emptyList()
        val openShiftTime = dateTimeBuilder.build(zxReportJson, "openShiftTime")
        val closeShiftTime = zxReportJson.readObject("closeShiftTime")?.let { dateTimeBuilder.build(zxReportJson, "closeShiftTime") }

        val reportWithoutChecksum = ZXReport(
            date_time = dateTime,
            shift_number = shiftNumber,
            sections = sections,
            operations = operations,
            discounts = discounts,
            markups = markups,
            total_result = totalResult,
            taxes = taxes,
            start_shift_non_nullable_sums = startShiftNonNullableSums,
            ticket_operations = ticketOperations,
            money_placements = moneyPlacements,
            annulled_tickets = annulledTickets,
            cash_sum = cashSum,
            revenue = revenue,
            non_nullable_sums = nonNullableSums,
            open_shift_time = openShiftTime,
            close_shift_time = closeShiftTime,
            checksum = ""
        )

        val checksum = computeChecksum(ZXReport.ADAPTER.encode(reportWithoutChecksum))
        return reportWithoutChecksum.copy(checksum = checksum)
    }

    /**
     * Строит Section из JSON.
     */
    private fun buildSection(sectionJson: JsonObject): ZXReport.Section {
        val sectionCode = sectionJson.readStringRequired("sectionCode")
        val operations = sectionJson.readObjectList("operations")
            ?: throw IllegalArgumentException("Missing operations / Отсутствует operations / operations өрісі жетіспейді")
        return ZXReport.Section(
            section_code = sectionCode,
            operations = operations.map { buildOperation(it) }
        )
    }

    /**
     * Строит Operation из JSON.
     */
    private fun buildOperation(operationJson: JsonObject): ZXReport.Operation {
        return ZXReport.Operation(
            operation = operationTypeBuilder.readRequired(operationJson, "operation"),
            count = operationJson.readIntRequired("count"),
            sum = moneyBuilder.build(operationJson.readObjectRequired("sum"))
        )
    }

    /**
     * Строит Tax из JSON.
     */
    private fun buildTax(taxJson: JsonObject): ZXReport.Tax {
        val taxType = taxJson.readIntRequired("taxType")
        val percent = taxJson.readIntRequired("percent")
        val operations = taxJson.readObjectList("operations")
            ?: throw IllegalArgumentException("Missing operations / Отсутствует operations / operations өрісі жетіспейді")
        return ZXReport.Tax(
            tax_type = taxType,
            percent = percent,
            operations = operations.map { buildTaxOperation(it) }
        )
    }

    /**
     * Строит TaxOperation из JSON.
     */
    private fun buildTaxOperation(taxOperationJson: JsonObject): ZXReport.Tax.TaxOperation {
        return ZXReport.Tax.TaxOperation(
            operation = operationTypeBuilder.readRequired(taxOperationJson, "operation"),
            turnover = moneyBuilder.build(taxOperationJson.readObjectRequired("turnover")),
            sum = moneyBuilder.build(taxOperationJson.readObjectRequired("sum")),
            turnover_without_tax = moneyBuilder.build(taxOperationJson.readObjectRequired("turnoverWithoutTax"))
        )
    }

    /**
     * Строит NonNullableSum из JSON.
     */
    private fun buildNonNullableSum(sumJson: JsonObject): ZXReport.NonNullableSum {
        return ZXReport.NonNullableSum(
            operation = operationTypeBuilder.readRequired(sumJson, "operation"),
            sum = moneyBuilder.build(sumJson.readObjectRequired("sum"))
        )
    }

    /**
     * Строит TicketOperation из JSON.
     */
    private fun buildTicketOperation(ticketJson: JsonObject): ZXReport.TicketOperation {
        val operation = operationTypeBuilder.readRequired(ticketJson, "operation")
        val ticketsTotalCount = ticketJson.readIntRequired("ticketsTotalCount")
        val ticketsCount = ticketJson.readIntRequired("ticketsCount")
        val ticketsSum = moneyBuilder.build(ticketJson.readObjectRequired("ticketsSum"))
        val payments = ticketJson.readObjectList("payments")
            ?: throw IllegalArgumentException("Missing payments / Отсутствует payments / payments өрісі жетіспейді")
        val offlineCount = ticketJson.readIntRequired("offlineCount")
        val discountSum = moneyBuilder.build(ticketJson.readObjectRequired("discountSum"))
        val markupSum = moneyBuilder.build(ticketJson.readObjectRequired("markupSum"))
        val changeSum = moneyBuilder.build(ticketJson.readObjectRequired("changeSum"))

        return ZXReport.TicketOperation(
            operation = operation,
            tickets_total_count = ticketsTotalCount,
            tickets_count = ticketsCount,
            tickets_sum = ticketsSum,
            payments = payments.map { buildTicketPayment(it) },
            offline_count = offlineCount,
            discount_sum = discountSum,
            markup_sum = markupSum,
            change_sum = changeSum
        )
    }

    /**
     * Строит TicketOperation.Payment из JSON.
     */
    private fun buildTicketPayment(paymentJson: JsonObject): ZXReport.TicketOperation.Payment {
        return ZXReport.TicketOperation.Payment(
            payment = paymentTypeBuilder.readRequired(paymentJson, "payment"),
            sum = moneyBuilder.build(paymentJson.readObjectRequired("sum")),
            count = paymentJson.readIntRequired("count")
        )
    }

    /**
     * Строит MoneyPlacement из JSON.
     */
    private fun buildMoneyPlacement(placementJson: JsonObject): ZXReport.MoneyPlacement {
        val op = placementJson.readStringRequired("operation")
        return ZXReport.MoneyPlacement(
            operation = MoneyPlacementEnum.valueOf(op),
            operations_total_count = placementJson.readIntRequired("operationsTotalCount"),
            operations_count = placementJson.readIntRequired("operationsCount"),
            operations_sum = moneyBuilder.build(placementJson.readObjectRequired("operationsSum")),
            offline_count = placementJson.readIntRequired("offlineCount")
        )
    }

    /**
     * Строит AnnulledTickets из JSON (deprecated).
     */
    private fun buildAnnulledTickets(annulledJson: JsonObject): ZXReport.AnnulledTickets {
        val annulledTicketsTotalCount = annulledJson.readIntRequired("annulledTicketsTotalCount")
        val annulledTicketsCount = annulledJson.readIntRequired("annulledTicketsCount")
        val annulledOperations = annulledJson.readObjectList("annulledOperations")?.map { buildOperation(it) } ?: emptyList()
        return ZXReport.AnnulledTickets(
            annulled_tickets_total_count = annulledTicketsTotalCount,
            annulled_tickets_count = annulledTicketsCount,
            annulled_operations = annulledOperations
        )
    }

    /**
     * Строит Revenue из JSON.
     */
    private fun buildRevenue(revenueJson: JsonObject): ZXReport.Revenue {
        return ZXReport.Revenue(
            sum = moneyBuilder.build(revenueJson.readObjectRequired("sum")),
            is_negative = revenueJson.readBoolRequired("isNegative")
        )
    }

    private fun computeChecksum(bytes: ByteArray): String {
        val crcValue = Crc32.calculate(bytes)
        return crcValue.toString(16).padStart(8, '0').uppercase()
    }
}
