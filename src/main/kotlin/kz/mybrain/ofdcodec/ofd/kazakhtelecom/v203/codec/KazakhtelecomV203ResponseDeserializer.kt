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
        return KazakhtelecomV203ReportDeserializerHelper.buildReportResponse(report)
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


}
