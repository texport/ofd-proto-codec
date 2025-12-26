package kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.codec

import kz.kazakhtelecom.proto.v203.Common
import kz.kazakhtelecom.proto.v203.Message
import kz.kazakhtelecom.proto.v203.Nomenclature
import kz.kazakhtelecom.proto.v203.Reginfo
import kz.kazakhtelecom.proto.v203.Report
import kz.kazakhtelecom.proto.v203.Service
import kz.kazakhtelecom.proto.v203.Ticket
import kz.mybrain.ofdcodec.domain.port.Deserializer
import kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.model.ResultType
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.util.Base64

/**
 * Десериализация ответов ОФД для протокола Казахтелеком 2.0.3.
 */
class KazakhtelecomV203ResponseDeserializer : Deserializer {
    /**
     * Десериализует proto Response в JSON-структуру.
     */
    override fun deserialize(bytes: ByteArray): JsonObject {
        val response = Message.Response.parser().parsePartialFrom(bytes)

        return buildJsonObject {
            put("commandType", JsonPrimitive(response.command.name))
            put(
                "result",
                buildJsonObject {
                    val resultCode = response.result.resultCode
                    put("resultCode", JsonPrimitive(resultCode))
                    ResultType.fromCode(resultCode)?.let { mapped ->
                        put(
                            "resultType",
                            buildJsonObject {
                                put("code", JsonPrimitive(mapped.code))
                                put("name", JsonPrimitive(mapped.title))
                                put("descriptionRu", JsonPrimitive(mapped.descriptionRu))
                                put("descriptionEn", JsonPrimitive(mapped.descriptionEn))
                            }
                        )
                    }
                    if (response.result.hasResultText()) {
                        put("resultText", JsonPrimitive(response.result.resultText))
                    }
                }
            )

            if (response.hasService()) {
                put("service", buildServiceResponse(response.service))
            }
            if (response.hasTicket()) {
                put("ticket", buildTicketResponse(response.ticket))
            }
            if (response.hasNomenclature()) {
                put("nomenclature", buildNomenclatureResponse(response.nomenclature))
            }
            if (response.hasReport()) {
                put("report", buildReportResponse(response.report))
            }
        }
    }

    /**
     * Преобразует ServiceResponse в JSON по спецификации протокола.
     */
    private fun buildServiceResponse(service: Service.ServiceResponse): JsonObject {
        return buildJsonObject {
            if (service.ticketAdsCount > 0) {
                put(
                    "ticketAds",
                    buildJsonArray {
                        service.ticketAdsList.forEach { add(buildTicketAd(it)) }
                    }
                )
            }
            if (service.hasRegInfo()) {
                put("regInfo", buildServiceRegInfo(service.regInfo))
            }
        }
    }

    /**
     * Преобразует TicketAd в JSON.
     */
    private fun buildTicketAd(ad: Common.TicketAd): JsonObject {
        return buildJsonObject {
            if (ad.hasInfo()) {
                put("info", buildTicketAdInfo(ad.info))
            }
            if (ad.hasText()) {
                put("text", JsonPrimitive(ad.text))
            }
        }
    }

    /**
     * Преобразует TicketAdInfo в JSON.
     */
    private fun buildTicketAdInfo(info: Common.TicketAdInfo): JsonObject {
        return buildJsonObject {
            put("type", JsonPrimitive(info.type.name))
            put("version", JsonPrimitive(info.version))
        }
    }

    /**
     * Преобразует ServiceResponse.RegInfo в JSON.
     */
    private fun buildServiceRegInfo(regInfo: Service.ServiceResponse.RegInfo): JsonObject {
        return buildJsonObject {
            if (regInfo.hasKkm()) {
                put("kkm", buildKkmRegInfo(regInfo.kkm))
            }
            if (regInfo.hasOrg()) {
                put("org", buildOrgRegInfo(regInfo.org))
            }
            if (regInfo.hasPos()) {
                put("pos", buildPosRegInfo(regInfo.pos))
            }
        }
    }

    /**
     * Преобразует TicketResponse в JSON.
     */
    private fun buildTicketResponse(ticket: Ticket.TicketResponse): JsonObject {
        return buildJsonObject {
            put("ticketNumber", JsonPrimitive(ticket.ticketNumber))
            if (ticket.hasQrCode()) {
                val encoded = Base64.getEncoder().encodeToString(ticket.qrCode.toByteArray())
                put("qrCodeBase64", JsonPrimitive(encoded))
            }
        }
    }

    /**
     * Преобразует KkmRegInfo в JSON.
     */
    private fun buildKkmRegInfo(kkm: Reginfo.KkmRegInfo): JsonObject {
        return buildJsonObject {
            if (kkm.hasPointOfPaymentNumber()) {
                put("pointOfPaymentNumber", JsonPrimitive(kkm.pointOfPaymentNumber))
            }
            if (kkm.hasTerminalNumber()) {
                put("terminalNumber", JsonPrimitive(kkm.terminalNumber))
            }
            if (kkm.hasFnsKkmId()) {
                put("fnsKkmId", JsonPrimitive(kkm.fnsKkmId))
            }
            if (kkm.hasSerialNumber()) {
                put("serialNumber", JsonPrimitive(kkm.serialNumber))
            }
            if (kkm.hasKkmId()) {
                put("kkmId", JsonPrimitive(kkm.kkmId))
            }
        }
    }

    /**
     * Преобразует OrgRegInfo в JSON.
     */
    private fun buildOrgRegInfo(org: Reginfo.OrgRegInfo): JsonObject {
        return buildJsonObject {
            if (org.hasTitle()) {
                put("title", JsonPrimitive(org.title))
            }
            if (org.hasAddress()) {
                put("address", JsonPrimitive(org.address))
            }
            if (org.hasAddressKz()) {
                put("addressKz", JsonPrimitive(org.addressKz))
            }
            if (org.hasInn()) {
                put("inn", JsonPrimitive(org.inn))
            }
            if (org.hasOkved()) {
                put("okved", JsonPrimitive(org.okved))
            }
        }
    }

    /**
     * Преобразует PosRegInfo в JSON.
     */
    private fun buildPosRegInfo(pos: Reginfo.PosRegInfo): JsonObject {
        return buildJsonObject {
            if (pos.hasTitle()) {
                put("title", JsonPrimitive(pos.title))
            }
            if (pos.hasAddress()) {
                put("address", JsonPrimitive(pos.address))
            }
            if (pos.hasAddressKz()) {
                put("addressKz", JsonPrimitive(pos.addressKz))
            }
            if (pos.hasLatitude()) {
                put("latitude", JsonPrimitive(pos.latitude))
            }
            if (pos.hasLongitude()) {
                put("longitude", JsonPrimitive(pos.longitude))
            }
        }
    }

    /**
     * Преобразует ReportResponse в JSON.
     */
    private fun buildReportResponse(report: Report.ReportResponse): JsonObject {
        return buildJsonObject {
            put("reportType", JsonPrimitive(report.report.name))
            if (report.hasZxReport()) {
                put("zxReport", buildZXReport(report.zxReport))
            }
        }
    }

    /**
     * Преобразует ZXReport в JSON.
     */
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

    /**
     * Преобразует NomenclatureResponse в JSON.
     */
    private fun buildNomenclatureResponse(nomenclature: Nomenclature.NomenclatureResponse): JsonObject {
        return buildJsonObject {
            put("version", JsonPrimitive(nomenclature.version))
            if (nomenclature.hasCreatedTime()) {
                put("createdTime", buildDateTime(nomenclature.createdTime))
            }
            if (nomenclature.elementsCount > 0) {
                put(
                    "elements",
                    buildJsonArray {
                        nomenclature.elementsList.forEach { add(buildNomenclatureElement(it)) }
                    }
                )
            }
            put(
                "result",
                buildJsonObject {
                    put("code", JsonPrimitive(nomenclature.result.number))
                    put("name", JsonPrimitive(nomenclature.result.name))
                }
            )
        }
    }

    /**
     * Преобразует NomenclatureResponse.Element в JSON.
     */
    private fun buildNomenclatureElement(element: Nomenclature.NomenclatureResponse.Element): JsonObject {
        return buildJsonObject {
            put("type", JsonPrimitive(element.type.name))
            put("title", JsonPrimitive(element.title))
            if (element.hasTitleKk()) {
                put("titleKk", JsonPrimitive(element.titleKk))
            }
            if (element.hasParentGroupId()) {
                put("parentGroupId", JsonPrimitive(element.parentGroupId))
            }
            put("id", JsonPrimitive(element.id))
            if (element.hasItem()) {
                put("item", buildNomenclatureItem(element.item))
            }
        }
    }

    /**
     * Преобразует NomenclatureResponse.Item в JSON.
     */
    private fun buildNomenclatureItem(item: Nomenclature.NomenclatureResponse.Item): JsonObject {
        return buildJsonObject {
            if (item.hasArticle()) {
                put("article", JsonPrimitive(item.article))
            }
            if (item.hasBarcode()) {
                put("barcode", JsonPrimitive(item.barcode))
            }
            if (item.hasDescription()) {
                put("description", JsonPrimitive(item.description))
            }
            if (item.hasPurchasePrice()) {
                put("purchasePrice", buildMoney(item.purchasePrice))
            }
            if (item.hasSellPrice()) {
                put("sellPrice", buildMoney(item.sellPrice))
            }
            if (item.hasDiscountPercent()) {
                put("discountPercent", JsonPrimitive(item.discountPercent))
            }
            if (item.hasDiscountSum()) {
                put("discountSum", buildMoney(item.discountSum))
            }
            if (item.hasMarkupPercent()) {
                put("markupPercent", JsonPrimitive(item.markupPercent))
            }
            if (item.hasMarkupSum()) {
                put("markupSum", buildMoney(item.markupSum))
            }
            if (item.taxesCount > 0) {
                put(
                    "taxes",
                    buildJsonArray {
                        item.taxesList.forEach { add(buildNomenclatureTax(it)) }
                    }
                )
            }
            if (item.hasMeasureCount()) {
                put("measureCount", JsonPrimitive(item.measureCount))
            }
            if (item.hasMeasureTitle()) {
                put("measureTitle", JsonPrimitive(item.measureTitle))
            }
            if (item.hasMeasureFractional()) {
                put("measureFractional", JsonPrimitive(item.measureFractional))
            }
            if (item.hasMeasureUnitCode()) {
                put("measureUnitCode", JsonPrimitive(item.measureUnitCode))
            }
            if (item.hasNtin()) {
                put("ntin", JsonPrimitive(item.ntin))
            }
            if (item.hasIsMarkedeac()) {
                put("isMarkedeac", JsonPrimitive(item.isMarkedeac))
            }
            if (item.hasIsSocial()) {
                put("isSocial", JsonPrimitive(item.isSocial))
            }
        }
    }

    /**
     * Преобразует NomenclatureResponse.Tax в JSON.
     */
    private fun buildNomenclatureTax(tax: Nomenclature.NomenclatureResponse.Tax): JsonObject {
        return buildJsonObject {
            put("taxationType", JsonPrimitive(tax.taxationType.number))
            put("taxType", JsonPrimitive(tax.taxType.number))
            put("taxPercent", JsonPrimitive(tax.taxPercent))
        }
    }

    /**
     * Преобразует Common.Money в JSON.
     */
    private fun buildMoney(money: Common.Money): JsonObject {
        return buildJsonObject {
            put("bills", JsonPrimitive(money.bills))
            put("coins", JsonPrimitive(money.coins))
        }
    }

    /**
     * Преобразует Common.DateTime в JSON.
     */
    private fun buildDateTime(dateTime: Common.DateTime): JsonObject {
        return buildJsonObject {
            put(
                "date",
                buildJsonObject {
                    put("year", JsonPrimitive(dateTime.date.year))
                    put("month", JsonPrimitive(dateTime.date.month))
                    put("day", JsonPrimitive(dateTime.date.day))
                }
            )
            put(
                "time",
                buildJsonObject {
                    put("hour", JsonPrimitive(dateTime.time.hour))
                    put("minute", JsonPrimitive(dateTime.time.minute))
                    if (dateTime.time.hasSecond()) {
                        put("second", JsonPrimitive(dateTime.time.second))
                    }
                }
            )
        }
    }
}
