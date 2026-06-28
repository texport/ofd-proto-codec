package kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.codec.ticket

import kz.kazakhtelecom.proto.v203.*

import kotlinx.serialization.json.JsonObject

import kz.mybrain.ofdcodec.infrastructure.json.readArray
import kz.mybrain.ofdcodec.infrastructure.json.readArrayRequired
import kz.mybrain.ofdcodec.infrastructure.json.readBoolRequired
import kz.mybrain.ofdcodec.infrastructure.json.readInt
import kz.mybrain.ofdcodec.infrastructure.json.readIntRequired
import kz.mybrain.ofdcodec.infrastructure.json.readLong
import kz.mybrain.ofdcodec.infrastructure.json.readLongRequired
import kz.mybrain.ofdcodec.infrastructure.json.readObject
import kz.mybrain.ofdcodec.infrastructure.json.readObjectRequired
import kz.mybrain.ofdcodec.infrastructure.json.readString
import kz.mybrain.ofdcodec.infrastructure.json.readStringElement
import kz.mybrain.ofdcodec.infrastructure.json.readStringRequired
import kz.mybrain.ofdcodec.infrastructure.json.requireObject
import kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.codec.common.DateTimeBuilder
import kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.codec.common.MoneyBuilder
import kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.codec.common.OperatorBuilder
import kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.codec.enums.OperationTypeBuilder
import kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.codec.enums.PaymentTypeBuilder
import kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.codec.enums.TicketItemTypeBuilder

/**
 * Сборщик TicketRequest из JSON-структуры.
 *
 * Ожидает в payload объект "ticket" и строит protobuf TicketRequest.
 * Domain намеренно игнорируется, так как в протоколе v203 этот блок не используется.
 */
internal class TicketRequestBuilder {
    private val dateTimeBuilder = DateTimeBuilder()
    private val moneyBuilder = MoneyBuilder()
    private val operatorBuilder = OperatorBuilder()
    private val operationTypeBuilder = OperationTypeBuilder()
    private val paymentTypeBuilder = PaymentTypeBuilder()
    private val itemTypeBuilder = TicketItemTypeBuilder()

    /**
     * Строит TicketRequest из JSON-объекта payload.
     */
    fun build(payload: JsonObject): TicketRequest {
        val ticketJson = payload["ticket"] as? JsonObject
            ?: throw IllegalArgumentException("Missing ticket / Отсутствует ticket / ticket өрісі жетіспейді")

        val items = ticketJson.readArrayRequired("items").map { buildItem(it.requireObject("items")) }
        val payments = ticketJson.readArray("payments")?.map { buildPayment(it.requireObject("payments")) } ?: emptyList()
        val taxes = ticketJson.readArray("taxes")?.map { buildTax(it.requireObject("taxes")) } ?: emptyList()
        val extensionOptions = ticketJson.readObject("extensionOptions")?.let { buildExtensionOptions(it) }
        val parentTicket = ticketJson.readObject("parentTicket")?.let { buildParentTicket(it) }

        return TicketRequest(
            operation = operationTypeBuilder.readRequired(ticketJson, "operation"),
            date_time = dateTimeBuilder.build(ticketJson, "dateTime"),
            operator_ = operatorBuilder.build(ticketJson.readObjectRequired("operator")),
            domain = null,
            items = items,
            payments = payments,
            taxes = taxes,
            amounts = buildAmounts(ticketJson.readObjectRequired("amounts")),
            extension_options = extensionOptions,
            offline_ticket_number = ticketJson.readInt("offlineTicketNumber"),
            printed_ticket = ticketJson.readString("printedTicket"),
            fr_shift_number = ticketJson.readInt("frShiftNumber"),
            shift_document_number = ticketJson.readInt("shiftDocumentNumber"),
            printed_document_number = ticketJson.readLong("printedDocumentNumber"),
            parent_ticket = parentTicket
        )
    }

    /**
     * Строит Item для TicketRequest.
     */
    private fun buildItem(itemJson: JsonObject): TicketRequest.Item {
        val type = itemTypeBuilder.readRequired(itemJson, "type")
        var commodity: TicketRequest.Item.Commodity? = null
        var stornoCommodity: TicketRequest.Item.StornoCommodity? = null
        var markup: TicketRequest.Modifier? = null
        var stornoMarkup: TicketRequest.Modifier? = null
        var discount: TicketRequest.Modifier? = null
        var stornoDiscount: TicketRequest.Modifier? = null

        when (type) {
            TicketRequest.Item.ItemTypeEnum.ITEM_TYPE_COMMODITY ->
                commodity = buildCommodity(itemJson.readObjectRequired("commodity"))
            TicketRequest.Item.ItemTypeEnum.ITEM_TYPE_STORNO_COMMODITY ->
                stornoCommodity = buildStornoCommodity(itemJson.readObjectRequired("stornoCommodity"))
            TicketRequest.Item.ItemTypeEnum.ITEM_TYPE_MARKUP ->
                markup = buildModifier(itemJson.readObjectRequired("markup"))
            TicketRequest.Item.ItemTypeEnum.ITEM_TYPE_STORNO_MARKUP ->
                stornoMarkup = buildModifier(itemJson.readObjectRequired("stornoMarkup"))
            TicketRequest.Item.ItemTypeEnum.ITEM_TYPE_DISCOUNT ->
                discount = buildModifier(itemJson.readObjectRequired("discount"))
            TicketRequest.Item.ItemTypeEnum.ITEM_TYPE_STORNO_DISCOUNT ->
                stornoDiscount = buildModifier(itemJson.readObjectRequired("stornoDiscount"))
        }

        return TicketRequest.Item(
            type = type,
            commodity = commodity,
            storno_commodity = stornoCommodity,
            markup = markup,
            storno_markup = stornoMarkup,
            discount = discount,
            storno_discount = stornoDiscount
        )
    }

    /**
     * Строит Commodity для TicketRequest.Item.
     */
    private fun buildCommodity(commodityJson: JsonObject): TicketRequest.Item.Commodity {
        val name = commodityJson.readString("name")
        val code = commodityJson.readLong("code")
        require(name != null || code != null) {
            "Missing name or code"
        }
        val taxes = commodityJson.readArray("taxes")?.map { buildTax(it.requireObject("taxes")) } ?: emptyList()
        val listExciseStamp = commodityJson.readArray("listExciseStamp")?.map { it.readStringElement() } ?: emptyList()

        return TicketRequest.Item.Commodity(
            code = code,
            name = name,
            section_code = commodityJson.readStringRequired("sectionCode"),
            quantity = commodityJson.readLongRequired("quantity"),
            price = moneyBuilder.build(commodityJson.readObjectRequired("price")),
            sum = moneyBuilder.build(commodityJson.readObjectRequired("sum")),
            taxes = taxes,
            physical_label = commodityJson.readString("physicalLabel"),
            product_id = commodityJson.readString("productId"),
            barcode = commodityJson.readString("barcode"),
            measure_unit_code = commodityJson.readString("measureUnitCode"),
            list_excise_stamp = listExciseStamp,
            ntin = commodityJson.readString("ntin")
        )
    }

    /**
     * Строит StornoCommodity для TicketRequest.Item.
     */
    private fun buildStornoCommodity(stornoJson: JsonObject): TicketRequest.Item.StornoCommodity {
        val taxes = stornoJson.readArray("taxes")?.map { buildTax(it.requireObject("taxes")) } ?: emptyList()
        val listExciseStamp = stornoJson.readArray("listExciseStamp")?.map { it.readStringElement() } ?: emptyList()

        return TicketRequest.Item.StornoCommodity(
            name = stornoJson.readString("name"),
            section_code = stornoJson.readStringRequired("sectionCode"),
            quantity = stornoJson.readLongRequired("quantity"),
            price = moneyBuilder.build(stornoJson.readObjectRequired("price")),
            sum = moneyBuilder.build(stornoJson.readObjectRequired("sum")),
            taxes = taxes,
            physical_label = stornoJson.readString("physicalLabel"),
            product_id = stornoJson.readString("productId"),
            barcode = stornoJson.readString("barcode"),
            measure_unit_code = stornoJson.readString("measureUnitCode"),
            list_excise_stamp = listExciseStamp,
            ntin = stornoJson.readString("ntin")
        )
    }

    /**
     * Строит Modifier (скидка/наценка/сторно).
     */
    private fun buildModifier(modifierJson: JsonObject): TicketRequest.Modifier {
        val taxes = modifierJson.readArray("taxes")?.map { buildTax(it.requireObject("taxes")) } ?: emptyList()
        return TicketRequest.Modifier(
            name = modifierJson.readStringRequired("name"),
            sum = moneyBuilder.build(modifierJson.readObjectRequired("sum")),
            taxes = taxes
        )
    }

    /**
     * Строит Tax для TicketRequest.
     */
    private fun buildTax(taxJson: JsonObject): TicketRequest.Tax {
        return TicketRequest.Tax(
            tax_type = taxJson.readIntRequired("taxType"),
            taxation_type = taxJson.readInt("taxationType"),
            percent = taxJson.readIntRequired("percent"),
            sum = moneyBuilder.build(taxJson.readObjectRequired("sum")),
            is_in_total_sum = taxJson.readBoolRequired("isInTotalSum")
        )
    }

    /**
     * Строит Payment для TicketRequest.
     */
    private fun buildPayment(paymentJson: JsonObject): TicketRequest.Payment {
        val cardPayment = paymentJson.readObject("cardPaymentFields")?.let { buildCardPaymentFields(it) }
        val mobilePayment = paymentJson.readObject("mobilePaymentFields")?.let { buildMobilePaymentFields(it) }
        return TicketRequest.Payment(
            type = paymentTypeBuilder.readRequired(paymentJson, "type"),
            sum = moneyBuilder.build(paymentJson.readObjectRequired("sum")),
            card_payment_fields = cardPayment,
            mobile_payment_fields = mobilePayment
        )
    }

    /**
     * Строит CardPaymentFields.
     */
    private fun buildCardPaymentFields(json: JsonObject): TicketRequest.Payment.CardPaymentFields {
        return TicketRequest.Payment.CardPaymentFields(
            pos_terminal_id = json.readString("posTerminalId"),
            pos_card_type = json.readString("posCardType"),
            pos_autorization_code = json.readInt("posAutorizationCode"),
            pos_rrn = json.readLong("posRrn"),
            pos_receipt_number = json.readInt("posReceiptNumber")
        )
    }

    /**
     * Строит MobilePaymentFields.
     */
    private fun buildMobilePaymentFields(json: JsonObject): TicketRequest.Payment.MobilePaymentFields {
        return TicketRequest.Payment.MobilePaymentFields(
            qr_type = json.readString("qrType"),
            qr_id = json.readString("qrId")
        )
    }

    /**
     * Строит Amounts для TicketRequest.
     */
    private fun buildAmounts(amountsJson: JsonObject): TicketRequest.Amounts {
        val taken = amountsJson.readObject("taken")?.let { moneyBuilder.build(it) }
        val change = amountsJson.readObject("change")?.let { moneyBuilder.build(it) }
        val markup = amountsJson.readObject("markup")?.let { buildModifier(it) }
        val discount = amountsJson.readObject("discount")?.let { buildModifier(it) }

        return TicketRequest.Amounts(
            total = moneyBuilder.build(amountsJson.readObjectRequired("total")),
            taken = taken,
            change = change,
            markup = markup,
            discount = discount
        )
    }

    /**
     * Строит ExtensionOptions для TicketRequest.
     */
    private fun buildExtensionOptions(json: JsonObject): TicketRequest.ExtensionOptions {
        return TicketRequest.ExtensionOptions(
            customer_email = json.readString("customerEmail"),
            customer_phone = json.readString("customerPhone"),
            customer_iin_or_bin = json.readString("customerIinOrBin")
        )
    }

    /**
     * Строит ParentTicket для TicketRequest.
     */
    private fun buildParentTicket(json: JsonObject): TicketRequest.ParentTicket {
        return TicketRequest.ParentTicket(
            parent_ticket_number = json.readStringRequired("parentTicketNumber"),
            parent_ticket_data_time = dateTimeBuilder.build(json, "parentTicketDateTime"),
            kgd_kkm_id = json.readStringRequired("kgdKkmId"),
            parent_ticket_total = moneyBuilder.build(json.readObjectRequired("parentTicketTotal")),
            parent_ticket_is_offline = json.readBoolRequired("parentTicketIsOffline")
        )
    }
}
