package kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.codec.ticket

import kotlinx.serialization.json.JsonObject
import kz.kazakhtelecom.proto.v203.Ticket
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
     *
     * Обязательные поля:
     * - operation, dateTime, operator;
     * - items (минимум один элемент);
     * - amounts.total.
     *
     * Условия:
     * - printedTicket, offlineTicketNumber, frShiftNumber, shiftDocumentNumber, printedDocumentNumber — опциональны;
     * - parentTicket передается только для операций возврата;
     * - payments и taxes опциональны (валидация взаимного исключения выполняется отдельно).
     */
    fun build(payload: JsonObject): Ticket.TicketRequest {
        val ticketJson = payload["ticket"] as? JsonObject
            ?: throw IllegalArgumentException("Missing ticket / Отсутствует ticket / ticket өрісі жетіспейді")

        val builder = Ticket.TicketRequest.newBuilder()
        builder.setOperation(operationTypeBuilder.readRequired(ticketJson, "operation"))
        builder.setDateTime(dateTimeBuilder.build(ticketJson, "dateTime"))

        val operatorJson = ticketJson.readObjectRequired("operator")
        builder.setOperator(operatorBuilder.build(operatorJson))

        // domain не используется в протоколе v203, поле игнорируется.

        val items = ticketJson.readArrayRequired("items")
        items.forEach { builder.addItems(buildItem(it.requireObject("items"))) }

        ticketJson.readArray("payments")?.forEach { builder.addPayments(buildPayment(it.requireObject("payments"))) }
        ticketJson.readArray("taxes")?.forEach { builder.addTaxes(buildTax(it.requireObject("taxes"))) }

        builder.setAmounts(buildAmounts(ticketJson.readObjectRequired("amounts")))

        ticketJson.readObject("extensionOptions")?.let { builder.setExtensionOptions(buildExtensionOptions(it)) }

        ticketJson.readInt("offlineTicketNumber")?.let { builder.setOfflineTicketNumber(it) }
        ticketJson.readString("printedTicket")?.let { builder.setPrintedTicket(it) }
        ticketJson.readInt("frShiftNumber")?.let { builder.setFrShiftNumber(it) }
        ticketJson.readInt("shiftDocumentNumber")?.let { builder.setShiftDocumentNumber(it) }
        ticketJson.readLong("printedDocumentNumber")?.let { builder.setPrintedDocumentNumber(it) }

        ticketJson.readObject("parentTicket")?.let { builder.setParentTicket(buildParentTicket(it)) }

        return builder.build()
    }

    /**
     * Строит Item для TicketRequest.
     */
    private fun buildItem(itemJson: JsonObject): Ticket.TicketRequest.Item {
        val builder = Ticket.TicketRequest.Item.newBuilder()
        val type = itemTypeBuilder.readRequired(itemJson, "type")
        builder.setType(type)
        when (type) {
            Ticket.TicketRequest.Item.ItemTypeEnum.ITEM_TYPE_COMMODITY ->
                builder.setCommodity(buildCommodity(itemJson.readObjectRequired("commodity")))
            Ticket.TicketRequest.Item.ItemTypeEnum.ITEM_TYPE_STORNO_COMMODITY ->
                builder.setStornoCommodity(buildStornoCommodity(itemJson.readObjectRequired("stornoCommodity")))
            Ticket.TicketRequest.Item.ItemTypeEnum.ITEM_TYPE_MARKUP ->
                builder.setMarkup(buildModifier(itemJson.readObjectRequired("markup")))
            Ticket.TicketRequest.Item.ItemTypeEnum.ITEM_TYPE_STORNO_MARKUP ->
                builder.setStornoMarkup(buildModifier(itemJson.readObjectRequired("stornoMarkup")))
            Ticket.TicketRequest.Item.ItemTypeEnum.ITEM_TYPE_DISCOUNT ->
                builder.setDiscount(buildModifier(itemJson.readObjectRequired("discount")))
            Ticket.TicketRequest.Item.ItemTypeEnum.ITEM_TYPE_STORNO_DISCOUNT ->
                builder.setStornoDiscount(buildModifier(itemJson.readObjectRequired("stornoDiscount")))
        }
        return builder.build()
    }

    /**
     * Строит Commodity для TicketRequest.Item.
     */
    private fun buildCommodity(commodityJson: JsonObject): Ticket.TicketRequest.Item.Commodity {
        val builder = Ticket.TicketRequest.Item.Commodity.newBuilder()
        val name = commodityJson.readString("name")
        val code = commodityJson.readLong("code")
        require(name != null || code != null) {
            "Missing name or code"
        }
        name?.let { builder.setName(it) }
        code?.let { builder.setCode(it) }
        builder.setSectionCode(commodityJson.readStringRequired("sectionCode"))
        builder.setQuantity(commodityJson.readLongRequired("quantity"))
        builder.setPrice(moneyBuilder.build(commodityJson.readObjectRequired("price")))
        builder.setSum(moneyBuilder.build(commodityJson.readObjectRequired("sum")))
        commodityJson.readArray("taxes")?.forEach { builder.addTaxes(buildTax(it.requireObject("taxes"))) }
        commodityJson.readArray("listExciseStamp")?.forEach { builder.addListExciseStamp(it.readStringElement()) }
        commodityJson.readString("physicalLabel")?.let { builder.setPhysicalLabel(it) }
        commodityJson.readString("productId")?.let { builder.setProductId(it) }
        commodityJson.readString("barcode")?.let { builder.setBarcode(it) }
        commodityJson.readString("measureUnitCode")?.let { builder.setMeasureUnitCode(it) }
        commodityJson.readString("ntin")?.let { builder.setNtin(it) }
        return builder.build()
    }

    /**
     * Строит StornoCommodity для TicketRequest.Item.
     */
    private fun buildStornoCommodity(stornoJson: JsonObject): Ticket.TicketRequest.Item.StornoCommodity {
        val builder = Ticket.TicketRequest.Item.StornoCommodity.newBuilder()
        stornoJson.readString("name")?.let { builder.setName(it) }
        builder.setSectionCode(stornoJson.readStringRequired("sectionCode"))
        builder.setQuantity(stornoJson.readLongRequired("quantity"))
        builder.setPrice(moneyBuilder.build(stornoJson.readObjectRequired("price")))
        builder.setSum(moneyBuilder.build(stornoJson.readObjectRequired("sum")))
        stornoJson.readArray("taxes")?.forEach { builder.addTaxes(buildTax(it.requireObject("taxes"))) }
        stornoJson.readArray("listExciseStamp")?.forEach { builder.addListExciseStamp(it.readStringElement()) }
        stornoJson.readString("physicalLabel")?.let { builder.setPhysicalLabel(it) }
        stornoJson.readString("productId")?.let { builder.setProductId(it) }
        stornoJson.readString("barcode")?.let { builder.setBarcode(it) }
        stornoJson.readString("measureUnitCode")?.let { builder.setMeasureUnitCode(it) }
        stornoJson.readString("ntin")?.let { builder.setNtin(it) }
        return builder.build()
    }

    /**
     * Строит Modifier (скидка/наценка/сторно).
     */
    private fun buildModifier(modifierJson: JsonObject): Ticket.TicketRequest.Modifier {
        val builder = Ticket.TicketRequest.Modifier.newBuilder()
        builder.setName(modifierJson.readStringRequired("name"))
        builder.setSum(moneyBuilder.build(modifierJson.readObjectRequired("sum")))
        modifierJson.readArray("taxes")?.forEach { builder.addTaxes(buildTax(it.requireObject("taxes"))) }
        return builder.build()
    }

    /**
     * Строит Tax для TicketRequest.
     */
    private fun buildTax(taxJson: JsonObject): Ticket.TicketRequest.Tax {
        val builder = Ticket.TicketRequest.Tax.newBuilder()
        builder.setTaxType(taxJson.readIntRequired("taxType"))
        taxJson.readInt("taxationType")?.let { builder.setTaxationType(it) }
        builder.setPercent(taxJson.readIntRequired("percent"))
        builder.setSum(moneyBuilder.build(taxJson.readObjectRequired("sum")))
        builder.setIsInTotalSum(taxJson.readBoolRequired("isInTotalSum"))
        return builder.build()
    }

    /**
     * Строит Payment для TicketRequest.
     */
    private fun buildPayment(paymentJson: JsonObject): Ticket.TicketRequest.Payment {
        val builder = Ticket.TicketRequest.Payment.newBuilder()
        builder.setType(paymentTypeBuilder.readRequired(paymentJson, "type"))
        builder.setSum(moneyBuilder.build(paymentJson.readObjectRequired("sum")))
        paymentJson.readObject("cardPaymentFields")?.let { builder.setCardPaymentFields(buildCardPaymentFields(it)) }
        paymentJson.readObject(
            "mobilePaymentFields"
        )?.let { builder.setMobilePaymentFields(buildMobilePaymentFields(it)) }
        return builder.build()
    }

    /**
     * Строит CardPaymentFields.
     */
    private fun buildCardPaymentFields(json: JsonObject): Ticket.TicketRequest.Payment.CardPaymentFields {
        val builder = Ticket.TicketRequest.Payment.CardPaymentFields.newBuilder()
        json.readString("posTerminalId")?.let { builder.setPosTerminalId(it) }
        json.readString("posCardType")?.let { builder.setPosCardType(it) }
        json.readInt("posAutorizationCode")?.let { builder.setPosAutorizationCode(it) }
        json.readLong("posRrn")?.let { builder.setPosRrn(it) }
        json.readInt("posReceiptNumber")?.let { builder.setPosReceiptNumber(it) }
        return builder.build()
    }

    /**
     * Строит MobilePaymentFields.
     */
    private fun buildMobilePaymentFields(json: JsonObject): Ticket.TicketRequest.Payment.MobilePaymentFields {
        val builder = Ticket.TicketRequest.Payment.MobilePaymentFields.newBuilder()
        json.readString("qrType")?.let { builder.setQrType(it) }
        json.readString("qrId")?.let { builder.setQrId(it) }
        return builder.build()
    }

    /**
     * Строит Amounts для TicketRequest.
     */
    private fun buildAmounts(amountsJson: JsonObject): Ticket.TicketRequest.Amounts {
        val builder = Ticket.TicketRequest.Amounts.newBuilder()
        builder.setTotal(moneyBuilder.build(amountsJson.readObjectRequired("total")))
        amountsJson.readObject("taken")?.let { builder.setTaken(moneyBuilder.build(it)) }
        amountsJson.readObject("change")?.let { builder.setChange(moneyBuilder.build(it)) }
        amountsJson.readObject("markup")?.let { builder.setMarkup(buildModifier(it)) }
        amountsJson.readObject("discount")?.let { builder.setDiscount(buildModifier(it)) }
        return builder.build()
    }

    /**
     * Строит ExtensionOptions для TicketRequest.
     */
    private fun buildExtensionOptions(json: JsonObject): Ticket.TicketRequest.ExtensionOptions {
        val builder = Ticket.TicketRequest.ExtensionOptions.newBuilder()
        json.readString("customerEmail")?.let { builder.setCustomerEmail(it) }
        json.readString("customerPhone")?.let { builder.setCustomerPhone(it) }
        json.readString("customerIinOrBin")?.let { builder.setCustomerIinOrBin(it) }
        return builder.build()
    }

    /**
     * Строит ParentTicket для TicketRequest.
     */
    private fun buildParentTicket(json: JsonObject): Ticket.TicketRequest.ParentTicket {
        return Ticket.TicketRequest.ParentTicket.newBuilder()
            .setParentTicketNumber(json.readStringRequired("parentTicketNumber"))
            .setParentTicketDataTime(dateTimeBuilder.build(json, "parentTicketDateTime"))
            .setKgdKkmId(json.readStringRequired("kgdKkmId"))
            .setParentTicketTotal(moneyBuilder.build(json.readObjectRequired("parentTicketTotal")))
            .setParentTicketIsOffline(json.readBoolRequired("parentTicketIsOffline"))
            .build()
    }
}
