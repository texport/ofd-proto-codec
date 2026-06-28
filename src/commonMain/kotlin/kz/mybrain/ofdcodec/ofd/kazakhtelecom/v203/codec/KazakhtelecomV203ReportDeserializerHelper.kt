package kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.codec

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kz.kazakhtelecom.proto.v203.*

internal object KazakhtelecomV203ReportDeserializerHelper {

    fun buildReportResponse(report: ReportResponse): JsonObject {
        return buildJsonObject {
            put("reportType", JsonPrimitive(report.report.name))
            val zxReport = report.zx_report
            if (zxReport != null) {
                put("zxReport", buildZXReport(zxReport))
            }
        }
    }

    private fun buildZXReport(zxReport: ZXReport): JsonObject {
        return buildJsonObject {
            put("dateTime", buildDateTime(zxReport.date_time))
            put("shiftNumber", JsonPrimitive(zxReport.shift_number))

            if (zxReport.sections.isNotEmpty()) {
                put("sections", buildJsonArray { zxReport.sections.forEach { add(buildZXSection(it)) } })
            }
            if (zxReport.operations.isNotEmpty()) {
                put("operations", buildJsonArray { zxReport.operations.forEach { add(buildZXOperation(it)) } })
            }
            if (zxReport.discounts.isNotEmpty()) {
                put("discounts", buildJsonArray { zxReport.discounts.forEach { add(buildZXOperation(it)) } })
            }
            if (zxReport.markups.isNotEmpty()) {
                put("markups", buildJsonArray { zxReport.markups.forEach { add(buildZXOperation(it)) } })
            }
            if (zxReport.total_result.isNotEmpty()) {
                put("totalResult", buildJsonArray { zxReport.total_result.forEach { add(buildZXOperation(it)) } })
            }
            if (zxReport.taxes.isNotEmpty()) {
                put("taxes", buildJsonArray { zxReport.taxes.forEach { add(buildZXTax(it)) } })
            }
            if (zxReport.start_shift_non_nullable_sums.isNotEmpty()) {
                put(
                    "startShiftNonNullableSums",
                    buildJsonArray { zxReport.start_shift_non_nullable_sums.forEach { add(buildZXNonNullableSum(it)) } }
                )
            }
            if (zxReport.ticket_operations.isNotEmpty()) {
                put(
                    "ticketOperations",
                    buildJsonArray { zxReport.ticket_operations.forEach { add(buildZXTicketOperation(it)) } }
                )
            }
            if (zxReport.money_placements.isNotEmpty()) {
                put(
                    "moneyPlacements",
                    buildJsonArray { zxReport.money_placements.forEach { add(buildZXMoneyPlacement(it)) } }
                )
            }
            val annulledTickets = zxReport.annulled_tickets
            if (annulledTickets != null) {
                put("annulledTickets", buildZXAnnulledTickets(annulledTickets))
            }
            put("cashSum", buildMoney(zxReport.cash_sum))
            put("revenue", buildZXRevenue(zxReport.revenue))
            if (zxReport.non_nullable_sums.isNotEmpty()) {
                put(
                    "nonNullableSums",
                    buildJsonArray { zxReport.non_nullable_sums.forEach { add(buildZXNonNullableSum(it)) } }
                )
            }
            val openShiftTime = zxReport.open_shift_time
            if (openShiftTime != null) {
                put("openShiftTime", buildDateTime(openShiftTime))
            }
            val closeShiftTime = zxReport.close_shift_time
            if (closeShiftTime != null) {
                put("closeShiftTime", buildDateTime(closeShiftTime))
            }
            val checksum = zxReport.checksum
            if (checksum != null) {
                put("checksum", JsonPrimitive(checksum))
            }
        }
    }

    private fun buildZXSection(section: ZXReport.Section): JsonObject {
        return buildJsonObject {
            put("sectionCode", JsonPrimitive(section.section_code))
            if (section.operations.isNotEmpty()) {
                put("operations", buildJsonArray { section.operations.forEach { add(buildZXOperation(it)) } })
            }
        }
    }

    private fun buildZXOperation(operation: ZXReport.Operation): JsonObject {
        return buildJsonObject {
            put("operation", JsonPrimitive(operation.operation.name))
            put("count", JsonPrimitive(operation.count))
            put("sum", buildMoney(operation.sum))
        }
    }

    private fun buildZXTax(tax: ZXReport.Tax): JsonObject {
        return buildJsonObject {
            put("taxType", JsonPrimitive(tax.tax_type))
            put("percent", JsonPrimitive(tax.percent))
            if (tax.operations.isNotEmpty()) {
                put("operations", buildJsonArray { tax.operations.forEach { add(buildZXTaxOperation(it)) } })
            }
        }
    }

    private fun buildZXTaxOperation(operation: ZXReport.Tax.TaxOperation): JsonObject {
        return buildJsonObject {
            put("operation", JsonPrimitive(operation.operation.name))
            put("turnover", buildMoney(operation.turnover))
            put("sum", buildMoney(operation.sum))
            val turnoverWithoutTax = operation.turnover_without_tax
            if (turnoverWithoutTax != null) {
                put("turnoverWithoutTax", buildMoney(turnoverWithoutTax))
            }
        }
    }

    private fun buildZXNonNullableSum(sum: ZXReport.NonNullableSum): JsonObject {
        return buildJsonObject {
            put("operation", JsonPrimitive(sum.operation.name))
            put("sum", buildMoney(sum.sum))
        }
    }

    private fun buildZXTicketOperation(operation: ZXReport.TicketOperation): JsonObject {
        return buildJsonObject {
            put("operation", JsonPrimitive(operation.operation.name))
            put("ticketsTotalCount", JsonPrimitive(operation.tickets_total_count))
            put("ticketsCount", JsonPrimitive(operation.tickets_count))
            put("ticketsSum", buildMoney(operation.tickets_sum))
            if (operation.payments.isNotEmpty()) {
                put("payments", buildJsonArray { operation.payments.forEach { add(buildZXTicketPayment(it)) } })
            }
            val offlineCount = operation.offline_count
            if (offlineCount != null) {
                put("offlineCount", JsonPrimitive(offlineCount))
            }
            val discountSum = operation.discount_sum
            if (discountSum != null) {
                put("discountSum", buildMoney(discountSum))
            }
            val markupSum = operation.markup_sum
            if (markupSum != null) {
                put("markupSum", buildMoney(markupSum))
            }
            val changeSum = operation.change_sum
            if (changeSum != null) {
                put("changeSum", buildMoney(changeSum))
            }
        }
    }

    private fun buildZXTicketPayment(payment: ZXReport.TicketOperation.Payment): JsonObject {
        return buildJsonObject {
            put("payment", JsonPrimitive(payment.payment.name))
            put("sum", buildMoney(payment.sum))
            val count = payment.count
            if (count != null) {
                put("count", JsonPrimitive(count))
            }
        }
    }

    private fun buildZXMoneyPlacement(placement: ZXReport.MoneyPlacement): JsonObject {
        return buildJsonObject {
            put("operation", JsonPrimitive(placement.operation.name))
            put("operationsTotalCount", JsonPrimitive(placement.operations_total_count))
            put("operationsCount", JsonPrimitive(placement.operations_count))
            put("operationsSum", buildMoney(placement.operations_sum))
            val offlineCount = placement.offline_count
            if (offlineCount != null) {
                put("offlineCount", JsonPrimitive(offlineCount))
            }
        }
    }

    private fun buildZXAnnulledTickets(tickets: ZXReport.AnnulledTickets): JsonObject {
        return buildJsonObject {
            put("annulledTicketsTotalCount", JsonPrimitive(tickets.annulled_tickets_total_count))
            put("annulledTicketsCount", JsonPrimitive(tickets.annulled_tickets_count))
            if (tickets.annulled_operations.isNotEmpty()) {
                put(
                    "annulledOperations",
                    buildJsonArray { tickets.annulled_operations.forEach { add(buildZXOperation(it)) } }
                )
            }
        }
    }

    private fun buildZXRevenue(revenue: ZXReport.Revenue): JsonObject {
        return buildJsonObject {
            put("sum", buildMoney(revenue.sum))
            put("isNegative", JsonPrimitive(revenue.is_negative))
        }
    }
}
