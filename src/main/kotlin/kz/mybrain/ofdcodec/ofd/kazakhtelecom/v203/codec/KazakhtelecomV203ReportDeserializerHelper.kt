package kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.codec

import kz.kazakhtelecom.proto.v203.Report
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

object KazakhtelecomV203ReportDeserializerHelper {

    fun buildReportResponse(report: Report.ReportResponse): JsonObject {
        return buildJsonObject {
            put("reportType", JsonPrimitive(report.report.name))
            if (report.hasZxReport()) {
                put("zxReport", buildZXReport(report.zxReport))
            }
        }
    }

    private fun buildZXReport(zxReport: Report.ZXReport): JsonObject {
        return buildJsonObject {
            put("dateTime", buildDateTime(zxReport.dateTime))
            put("shiftNumber", JsonPrimitive(zxReport.shiftNumber))

            if (zxReport.sectionsCount > 0) {
                put("sections", buildJsonArray { zxReport.sectionsList.forEach { add(buildZXSection(it)) } })
            }
            if (zxReport.operationsCount > 0) {
                put("operations", buildJsonArray { zxReport.operationsList.forEach { add(buildZXOperation(it)) } })
            }
            if (zxReport.discountsCount > 0) {
                put("discounts", buildJsonArray { zxReport.discountsList.forEach { add(buildZXOperation(it)) } })
            }
            if (zxReport.markupsCount > 0) {
                put("markups", buildJsonArray { zxReport.markupsList.forEach { add(buildZXOperation(it)) } })
            }
            if (zxReport.totalResultCount > 0) {
                put("totalResult", buildJsonArray { zxReport.totalResultList.forEach { add(buildZXOperation(it)) } })
            }
            if (zxReport.taxesCount > 0) {
                put("taxes", buildJsonArray { zxReport.taxesList.forEach { add(buildZXTax(it)) } })
            }
            if (zxReport.startShiftNonNullableSumsCount > 0) {
                put(
                    "startShiftNonNullableSums",
                    buildJsonArray { zxReport.startShiftNonNullableSumsList.forEach { add(buildZXNonNullableSum(it)) } }
                )
            }
            if (zxReport.ticketOperationsCount > 0) {
                put("ticketOperations", buildJsonArray { zxReport.ticketOperationsList.forEach { add(buildZXTicketOperation(it)) } })
            }
            if (zxReport.moneyPlacementsCount > 0) {
                put("moneyPlacements", buildJsonArray { zxReport.moneyPlacementsList.forEach { add(buildZXMoneyPlacement(it)) } })
            }
            if (zxReport.hasAnnulledTickets()) {
                put("annulledTickets", buildZXAnnulledTickets(zxReport.annulledTickets))
            }
            put("cashSum", buildMoney(zxReport.cashSum))
            put("revenue", buildZXRevenue(zxReport.revenue))
            if (zxReport.nonNullableSumsCount > 0) {
                put("nonNullableSums", buildJsonArray { zxReport.nonNullableSumsList.forEach { add(buildZXNonNullableSum(it)) } })
            }
            if (zxReport.hasOpenShiftTime()) {
                put("openShiftTime", buildDateTime(zxReport.openShiftTime))
            }
            if (zxReport.hasCloseShiftTime()) {
                put("closeShiftTime", buildDateTime(zxReport.closeShiftTime))
            }
            if (zxReport.hasChecksum()) {
                put("checksum", JsonPrimitive(zxReport.checksum))
            }
        }
    }

    private fun buildZXSection(section: Report.ZXReport.Section): JsonObject {
        return buildJsonObject {
            put("sectionCode", JsonPrimitive(section.sectionCode))
            if (section.operationsCount > 0) {
                put("operations", buildJsonArray { section.operationsList.forEach { add(buildZXOperation(it)) } })
            }
        }
    }

    private fun buildZXOperation(operation: Report.ZXReport.Operation): JsonObject {
        return buildJsonObject {
            put("operation", JsonPrimitive(operation.operation.name))
            put("count", JsonPrimitive(operation.count))
            put("sum", buildMoney(operation.sum))
        }
    }

    private fun buildZXTax(tax: Report.ZXReport.Tax): JsonObject {
        return buildJsonObject {
            put("taxType", JsonPrimitive(tax.taxType))
            put("percent", JsonPrimitive(tax.percent))
            if (tax.operationsCount > 0) {
                put("operations", buildJsonArray { tax.operationsList.forEach { add(buildZXTaxOperation(it)) } })
            }
        }
    }

    private fun buildZXTaxOperation(operation: Report.ZXReport.Tax.TaxOperation): JsonObject {
        return buildJsonObject {
            put("operation", JsonPrimitive(operation.operation.name))
            put("turnover", buildMoney(operation.turnover))
            put("sum", buildMoney(operation.sum))
            if (operation.hasTurnoverWithoutTax()) {
                put("turnoverWithoutTax", buildMoney(operation.turnoverWithoutTax))
            }
        }
    }

    private fun buildZXNonNullableSum(sum: Report.ZXReport.NonNullableSum): JsonObject {
        return buildJsonObject {
            put("operation", JsonPrimitive(sum.operation.name))
            put("sum", buildMoney(sum.sum))
        }
    }

    private fun buildZXTicketOperation(operation: Report.ZXReport.TicketOperation): JsonObject {
        return buildJsonObject {
            put("operation", JsonPrimitive(operation.operation.name))
            put("ticketsTotalCount", JsonPrimitive(operation.ticketsTotalCount))
            put("ticketsCount", JsonPrimitive(operation.ticketsCount))
            put("ticketsSum", buildMoney(operation.ticketsSum))
            if (operation.paymentsCount > 0) {
                put("payments", buildJsonArray { operation.paymentsList.forEach { add(buildZXTicketPayment(it)) } })
            }
            if (operation.hasOfflineCount()) {
                put("offlineCount", JsonPrimitive(operation.offlineCount))
            }
            if (operation.hasDiscountSum()) {
                put("discountSum", buildMoney(operation.discountSum))
            }
            if (operation.hasMarkupSum()) {
                put("markupSum", buildMoney(operation.markupSum))
            }
            if (operation.hasChangeSum()) {
                put("changeSum", buildMoney(operation.changeSum))
            }
        }
    }

    private fun buildZXTicketPayment(payment: Report.ZXReport.TicketOperation.Payment): JsonObject {
        return buildJsonObject {
            put("payment", JsonPrimitive(payment.payment.name))
            put("sum", buildMoney(payment.sum))
            if (payment.hasCount()) {
                put("count", JsonPrimitive(payment.count))
            }
        }
    }

    private fun buildZXMoneyPlacement(placement: Report.ZXReport.MoneyPlacement): JsonObject {
        return buildJsonObject {
            put("operation", JsonPrimitive(placement.operation.name))
            put("operationsTotalCount", JsonPrimitive(placement.operationsTotalCount))
            put("operationsCount", JsonPrimitive(placement.operationsCount))
            put("operationsSum", buildMoney(placement.operationsSum))
            if (placement.hasOfflineCount()) {
                put("offlineCount", JsonPrimitive(placement.offlineCount))
            }
        }
    }

    private fun buildZXAnnulledTickets(tickets: Report.ZXReport.AnnulledTickets): JsonObject {
        return buildJsonObject {
            put("annulledTicketsTotalCount", JsonPrimitive(tickets.annulledTicketsTotalCount))
            put("annulledTicketsCount", JsonPrimitive(tickets.annulledTicketsCount))
            if (tickets.annulledOperationsCount > 0) {
                put("annulledOperations", buildJsonArray { tickets.annulledOperationsList.forEach { add(buildZXOperation(it)) } })
            }
        }
    }

    private fun buildZXRevenue(revenue: Report.ZXReport.Revenue): JsonObject {
        return buildJsonObject {
            put("sum", buildMoney(revenue.sum))
            put("isNegative", JsonPrimitive(revenue.isNegative))
        }
    }
}
