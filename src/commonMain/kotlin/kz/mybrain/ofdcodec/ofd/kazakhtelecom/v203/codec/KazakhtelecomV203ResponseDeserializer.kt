package kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.codec

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kz.kazakhtelecom.proto.v203.*
import kz.mybrain.ofdcodec.domain.port.Deserializer
import kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.model.ResultType
import kotlin.io.encoding.Base64

/**
 * Десериализация ответов ОФД для текущего provider module 2.0.3.
 */
internal class KazakhtelecomV203ResponseDeserializer : Deserializer {
    /**
     * Десериализует proto Response в JSON-структуру.
     */
    override fun deserialize(bytes: ByteArray): JsonObject {
        val response = Response.ADAPTER.decode(bytes)

        return buildJsonObject {
            put("commandType", JsonPrimitive(response.command.name))
            put(
                "result",
                buildJsonObject {
                    val resultCode = response.result.result_code
                    put("resultCode", JsonPrimitive(resultCode))
                    ResultType.fromCode(resultCode)?.let { mapped ->
                        put(
                            "resultType",
                            buildJsonObject {
                                put("code", JsonPrimitive(mapped.code))
                                put("name", JsonPrimitive(mapped.title))
                                put("descriptionRu", JsonPrimitive(mapped.descriptionRu))
                                put("descriptionKz", JsonPrimitive(mapped.descriptionKz))
                                put("descriptionEn", JsonPrimitive(mapped.descriptionEn))
                            }
                        )
                    }
                    val resultText = response.result.result_text
                    if (resultText != null) {
                        put("resultText", JsonPrimitive(resultText))
                    }
                }
            )

            val service = response.service
            if (service != null) {
                put("service", buildServiceResponse(service))
            }
            val ticket = response.ticket
            if (ticket != null) {
                put("ticket", buildTicketResponse(ticket))
            }
            val nomenclature = response.nomenclature
            if (nomenclature != null) {
                put("nomenclature", buildNomenclatureResponse(nomenclature))
            }
            val report = response.report
            if (report != null) {
                put("report", buildReportResponse(report))
            }
            val auth = response.auth
            if (auth != null) {
                put("auth", buildAuthResponse(auth))
            }
        }
    }

    /**
     * Преобразует ServiceResponse в JSON по спецификации протокола.
     */
    private fun buildServiceResponse(service: ServiceResponse): JsonObject {
        return buildJsonObject {
            if (service.ticket_ads.isNotEmpty()) {
                put(
                    "ticketAds",
                    buildJsonArray {
                        service.ticket_ads.forEach { add(buildTicketAd(it)) }
                    }
                )
            }
            val regInfo = service.reg_info
            if (regInfo != null) {
                put("regInfo", buildServiceRegInfo(regInfo))
            }
        }
    }

    /**
     * Преобразует TicketAd в JSON.
     */
    private fun buildTicketAd(ad: TicketAd): JsonObject {
        return buildJsonObject {
            put("info", buildTicketAdInfo(ad.info))
            put("text", JsonPrimitive(ad.text))
        }
    }

    /**
     * Преобразует TicketAdInfo в JSON.
     */
    private fun buildTicketAdInfo(info: TicketAdInfo): JsonObject {
        return buildJsonObject {
            put("type", JsonPrimitive(info.type.name))
            put("version", JsonPrimitive(info.version))
        }
    }

    /**
     * Преобразует ServiceResponse.RegInfo в JSON.
     */
    private fun buildServiceRegInfo(regInfo: ServiceResponse.RegInfo): JsonObject {
        return buildJsonObject {
            val kkm = regInfo.kkm
            if (kkm != null) {
                put("kkm", buildKkmRegInfo(kkm))
            }
            val org = regInfo.org
            if (org != null) {
                put("org", buildOrgRegInfo(org))
            }
            val pos = regInfo.pos
            if (pos != null) {
                put("pos", buildPosRegInfo(pos))
            }
        }
    }

    /**
     * Преобразует TicketResponse в JSON.
     */
    private fun buildTicketResponse(ticket: TicketResponse): JsonObject {
        return buildJsonObject {
            put("ticketNumber", JsonPrimitive(ticket.ticket_number))
            val qrCode = ticket.qr_code
            if (qrCode != null) {
                @OptIn(kotlin.io.encoding.ExperimentalEncodingApi::class)
                val encoded = Base64.encode(qrCode.toByteArray())
                put("qrCodeBase64", JsonPrimitive(encoded))
            }
        }
    }

    /**
     * Преобразует KkmRegInfo в JSON.
     */
    private fun buildKkmRegInfo(kkm: KkmRegInfo): JsonObject {
        return buildJsonObject {
            if (kkm.point_of_payment_number != null) {
                put("pointOfPaymentNumber", JsonPrimitive(kkm.point_of_payment_number))
            }
            if (kkm.terminal_number != null) {
                put("terminalNumber", JsonPrimitive(kkm.terminal_number))
            }
            if (kkm.fns_kkm_id != null) {
                put("fnsKkmId", JsonPrimitive(kkm.fns_kkm_id))
            }
            if (kkm.serial_number != null) {
                put("serialNumber", JsonPrimitive(kkm.serial_number))
            }
            if (kkm.kkm_id != null) {
                put("kkmId", JsonPrimitive(kkm.kkm_id))
            }
        }
    }

    /**
     * Преобразует OrgRegInfo в JSON.
     */
    private fun buildOrgRegInfo(org: OrgRegInfo): JsonObject {
        return buildJsonObject {
            if (org.title != null) {
                put("title", JsonPrimitive(org.title))
            }
            if (org.address != null) {
                put("address", JsonPrimitive(org.address))
            }
            if (org.address_kz != null) {
                put("addressKz", JsonPrimitive(org.address_kz))
            }
            if (org.inn != null) {
                put("inn", JsonPrimitive(org.inn))
            }
            if (org.okved != null) {
                put("okved", JsonPrimitive(org.okved))
            }
        }
    }

    /**
     * Преобразует PosRegInfo в JSON.
     */
    private fun buildPosRegInfo(pos: PosRegInfo): JsonObject {
        return buildJsonObject {
            if (pos.title != null) {
                put("title", JsonPrimitive(pos.title))
            }
            if (pos.address != null) {
                put("address", JsonPrimitive(pos.address))
            }
            if (pos.address_kz != null) {
                put("addressKz", JsonPrimitive(pos.address_kz))
            }
            if (pos.latitude != null) {
                put("latitude", JsonPrimitive(pos.latitude))
            }
            if (pos.longitude != null) {
                put("longitude", JsonPrimitive(pos.longitude))
            }
        }
    }

    /**
     * Преобразует ReportResponse в JSON.
     */
    private fun buildReportResponse(report: ReportResponse): JsonObject {
        return KazakhtelecomV203ReportDeserializerHelper.buildReportResponse(report)
    }

    /**
     * Преобразует NomenclatureResponse в JSON.
     */
    private fun buildNomenclatureResponse(nomenclature: NomenclatureResponse): JsonObject {
        return buildJsonObject {
            put("version", JsonPrimitive(nomenclature.version))
            val createdTime = nomenclature.created_time
            if (createdTime != null) {
                put("createdTime", buildDateTime(createdTime))
            }
            if (nomenclature.elements.isNotEmpty()) {
                put(
                    "elements",
                    buildJsonArray {
                        nomenclature.elements.forEach { add(buildNomenclatureElement(it)) }
                    }
                )
            }
            put(
                "result",
                buildJsonObject {
                    put("code", JsonPrimitive(nomenclature.result.value))
                    put("name", JsonPrimitive(nomenclature.result.name))
                }
            )
        }
    }

    /**
     * Преобразует NomenclatureResponse.Element в JSON.
     */
    private fun buildNomenclatureElement(element: NomenclatureResponse.Element): JsonObject {
        return buildJsonObject {
            put("type", JsonPrimitive(element.type.name))
            put("title", JsonPrimitive(element.title))
            if (element.title_kk != null) {
                put("titleKk", JsonPrimitive(element.title_kk))
            }
            if (element.parent_group_id != null) {
                put("parentGroupId", JsonPrimitive(element.parent_group_id))
            }
            put("id", JsonPrimitive(element.id))
            val item = element.item
            if (item != null) {
                put("item", buildNomenclatureItem(item))
            }
        }
    }

    /**
     * Преобразует NomenclatureResponse.Item в JSON.
     */
    private fun buildNomenclatureItem(item: NomenclatureResponse.Item): JsonObject {
        return buildJsonObject {
            if (item.article != null) {
                put("article", JsonPrimitive(item.article))
            }
            if (item.barcode != null) {
                put("barcode", JsonPrimitive(item.barcode))
            }
            if (item.description != null) {
                put("description", JsonPrimitive(item.description))
            }
            val purchasePrice = item.purchase_price
            if (purchasePrice != null) {
                put("purchasePrice", buildMoney(purchasePrice))
            }
            val sellPrice = item.sell_price
            if (sellPrice != null) {
                put("sellPrice", buildMoney(sellPrice))
            }
            if (item.discount_percent != null) {
                put("discountPercent", JsonPrimitive(item.discount_percent))
            }
            val discountSum = item.discount_sum
            if (discountSum != null) {
                put("discountSum", buildMoney(discountSum))
            }
            if (item.markup_percent != null) {
                put("markupPercent", JsonPrimitive(item.markup_percent))
            }
            val markupSum = item.markup_sum
            if (markupSum != null) {
                put("markupSum", buildMoney(markupSum))
            }
            if (item.taxes.isNotEmpty()) {
                put(
                    "taxes",
                    buildJsonArray {
                        item.taxes.forEach { add(buildNomenclatureTax(it)) }
                    }
                )
            }
            if (item.measure_count != null) {
                put("measureCount", JsonPrimitive(item.measure_count))
            }
            if (item.measure_title != null) {
                put("measureTitle", JsonPrimitive(item.measure_title))
            }
            if (item.measure_fractional != null) {
                put("measureFractional", JsonPrimitive(item.measure_fractional))
            }
            if (item.measure_unit_code != null) {
                put("measureUnitCode", JsonPrimitive(item.measure_unit_code))
            }
            if (item.ntin != null) {
                put("ntin", JsonPrimitive(item.ntin))
            }
            if (item.is_markedeac != null) {
                put("isMarkedeac", JsonPrimitive(item.is_markedeac))
            }
            if (item.is_social != null) {
                put("isSocial", JsonPrimitive(item.is_social))
            }
        }
    }

    /**
     * Преобразует NomenclatureResponse.Tax в JSON.
     */
    private fun buildNomenclatureTax(tax: NomenclatureResponse.Tax): JsonObject {
        return buildJsonObject {
            put("taxationType", JsonPrimitive(tax.taxation_type.value))
            put("taxType", JsonPrimitive(tax.tax_type.value))
            put("taxPercent", JsonPrimitive(tax.tax_percent))
        }
    }

    /**
     * Преобразует AuthResponse в JSON.
     */
    private fun buildAuthResponse(auth: AuthResponse): JsonObject {
        return buildJsonObject {
            put("result", JsonPrimitive(auth.result.name))
            if (auth.operator_code != null) {
                put("operatorCode", JsonPrimitive(auth.operator_code))
            }
            if (auth.operator_name != null) {
                put("operatorName", JsonPrimitive(auth.operator_name))
            }
            if (auth.roles.isNotEmpty()) {
                put(
                    "roles",
                    buildJsonArray {
                        auth.roles.forEach { add(JsonPrimitive(it.name)) }
                    }
                )
            }
        }
    }
}
