package kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.codec.ticket

import kz.kazakhtelecom.proto.v203.Ticket
import kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.codec.common.DateTimeBuilder
import kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.codec.common.MoneyBuilder
import kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.codec.common.OperatorBuilder
import kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.codec.enums.OperationTypeBuilder
import kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.codec.enums.PaymentTypeBuilder
import kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.codec.enums.TicketItemTypeBuilder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.longOrNull

/**
 * Сборщик TicketRequest из JSON-структуры.
 *
 * Ожидает в payload объект "ticket" и строит protobuf TicketRequest.
 * Domain намеренно игнорируется, так как в протоколе v203 этот блок не используется.
 */
class TicketRequestBuilder {
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
            ?: throw IllegalArgumentException("Missing ticket")

        val builder = Ticket.TicketRequest.newBuilder()
        builder.setOperation(operationTypeBuilder.readRequired(ticketJson, "operation"))
        builder.setDateTime(dateTimeBuilder.build(ticketJson, "dateTime"))

        val operatorJson = readObjectRequired(ticketJson, "operator")
        builder.setOperator(operatorBuilder.build(operatorJson))

        // domain не используется в протоколе v203, поле игнорируется.

        val items = readArrayRequired(ticketJson, "items")
        items.forEach { builder.addItems(buildItem(requireObject(it, "items"))) }

        readArray(ticketJson, "payments")?.forEach { builder.addPayments(buildPayment(requireObject(it, "payments"))) }
        readArray(ticketJson, "taxes")?.forEach { builder.addTaxes(buildTax(requireObject(it, "taxes"))) }

        builder.setAmounts(buildAmounts(readObjectRequired(ticketJson, "amounts")))

        readObject(ticketJson, "extensionOptions")?.let { builder.setExtensionOptions(buildExtensionOptions(it)) }

        readInt(ticketJson, "offlineTicketNumber")?.let { builder.setOfflineTicketNumber(it) }
        readString(ticketJson, "printedTicket")?.let { builder.setPrintedTicket(it) }
        readInt(ticketJson, "frShiftNumber")?.let { builder.setFrShiftNumber(it) }
        readInt(ticketJson, "shiftDocumentNumber")?.let { builder.setShiftDocumentNumber(it) }
        readLong(ticketJson, "printedDocumentNumber")?.let { builder.setPrintedDocumentNumber(it) }

        readObject(ticketJson, "parentTicket")?.let { builder.setParentTicket(buildParentTicket(it)) }

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
                builder.setCommodity(buildCommodity(readObjectRequired(itemJson, "commodity")))
            Ticket.TicketRequest.Item.ItemTypeEnum.ITEM_TYPE_STORNO_COMMODITY ->
                builder.setStornoCommodity(buildStornoCommodity(readObjectRequired(itemJson, "stornoCommodity")))
            Ticket.TicketRequest.Item.ItemTypeEnum.ITEM_TYPE_MARKUP ->
                builder.setMarkup(buildModifier(readObjectRequired(itemJson, "markup")))
            Ticket.TicketRequest.Item.ItemTypeEnum.ITEM_TYPE_STORNO_MARKUP ->
                builder.setStornoMarkup(buildModifier(readObjectRequired(itemJson, "stornoMarkup")))
            Ticket.TicketRequest.Item.ItemTypeEnum.ITEM_TYPE_DISCOUNT ->
                builder.setDiscount(buildModifier(readObjectRequired(itemJson, "discount")))
            Ticket.TicketRequest.Item.ItemTypeEnum.ITEM_TYPE_STORNO_DISCOUNT ->
                builder.setStornoDiscount(buildModifier(readObjectRequired(itemJson, "stornoDiscount")))
        }
        return builder.build()
    }

    /**
     * Строит Commodity для TicketRequest.Item.
     */
    private fun buildCommodity(commodityJson: JsonObject): Ticket.TicketRequest.Item.Commodity {
        val builder = Ticket.TicketRequest.Item.Commodity.newBuilder()
        val name = readString(commodityJson, "name")
        val code = readLong(commodityJson, "code")
        require(name != null || code != null) {
            "Missing name or code"
        }
        name?.let { builder.setName(it) }
        code?.let { builder.setCode(it) }
        builder.setSectionCode(readStringRequired(commodityJson, "sectionCode"))
        builder.setQuantity(readLongRequired(commodityJson, "quantity"))
        builder.setPrice(moneyBuilder.build(readObjectRequired(commodityJson, "price")))
        builder.setSum(moneyBuilder.build(readObjectRequired(commodityJson, "sum")))
        readArray(commodityJson, "taxes")?.forEach { builder.addTaxes(buildTax(requireObject(it, "taxes"))) }
        readArray(commodityJson, "listExciseStamp")?.forEach { builder.addListExciseStamp(readStringElement(it)) }
        readString(commodityJson, "physicalLabel")?.let { builder.setPhysicalLabel(it) }
        readString(commodityJson, "productId")?.let { builder.setProductId(it) }
        readString(commodityJson, "barcode")?.let { builder.setBarcode(it) }
        readString(commodityJson, "measureUnitCode")?.let { builder.setMeasureUnitCode(it) }
        readString(commodityJson, "ntin")?.let { builder.setNtin(it) }
        return builder.build()
    }

    /**
     * Строит StornoCommodity для TicketRequest.Item.
     */
    private fun buildStornoCommodity(stornoJson: JsonObject): Ticket.TicketRequest.Item.StornoCommodity {
        val builder = Ticket.TicketRequest.Item.StornoCommodity.newBuilder()
        readString(stornoJson, "name")?.let { builder.setName(it) }
        builder.setSectionCode(readStringRequired(stornoJson, "sectionCode"))
        builder.setQuantity(readLongRequired(stornoJson, "quantity"))
        builder.setPrice(moneyBuilder.build(readObjectRequired(stornoJson, "price")))
        builder.setSum(moneyBuilder.build(readObjectRequired(stornoJson, "sum")))
        readArray(stornoJson, "taxes")?.forEach { builder.addTaxes(buildTax(requireObject(it, "taxes"))) }
        readArray(stornoJson, "listExciseStamp")?.forEach { builder.addListExciseStamp(readStringElement(it)) }
        readString(stornoJson, "physicalLabel")?.let { builder.setPhysicalLabel(it) }
        readString(stornoJson, "productId")?.let { builder.setProductId(it) }
        readString(stornoJson, "barcode")?.let { builder.setBarcode(it) }
        readString(stornoJson, "measureUnitCode")?.let { builder.setMeasureUnitCode(it) }
        readString(stornoJson, "ntin")?.let { builder.setNtin(it) }
        return builder.build()
    }

    /**
     * Строит Modifier (скидка/наценка/сторно).
     */
    private fun buildModifier(modifierJson: JsonObject): Ticket.TicketRequest.Modifier {
        val builder = Ticket.TicketRequest.Modifier.newBuilder()
        builder.setName(readStringRequired(modifierJson, "name"))
        builder.setSum(moneyBuilder.build(readObjectRequired(modifierJson, "sum")))
        readArray(modifierJson, "taxes")?.forEach { builder.addTaxes(buildTax(requireObject(it, "taxes"))) }
        return builder.build()
    }

    /**
     * Строит Tax для TicketRequest.
     */
    private fun buildTax(taxJson: JsonObject): Ticket.TicketRequest.Tax {
        val builder = Ticket.TicketRequest.Tax.newBuilder()
        builder.setTaxType(readIntRequired(taxJson, "taxType"))
        readInt(taxJson, "taxationType")?.let { builder.setTaxationType(it) }
        builder.setPercent(readIntRequired(taxJson, "percent"))
        builder.setSum(moneyBuilder.build(readObjectRequired(taxJson, "sum")))
        builder.setIsInTotalSum(readBoolRequired(taxJson, "isInTotalSum"))
        return builder.build()
    }

    /**
     * Строит Payment для TicketRequest.
     */
    private fun buildPayment(paymentJson: JsonObject): Ticket.TicketRequest.Payment {
        val builder = Ticket.TicketRequest.Payment.newBuilder()
        builder.setType(paymentTypeBuilder.readRequired(paymentJson, "type"))
        builder.setSum(moneyBuilder.build(readObjectRequired(paymentJson, "sum")))
        readObject(paymentJson, "cardPaymentFields")?.let { builder.setCardPaymentFields(buildCardPaymentFields(it)) }
        readObject(paymentJson, "mobilePaymentFields")?.let { builder.setMobilePaymentFields(buildMobilePaymentFields(it)) }
        return builder.build()
    }

    /**
     * Строит CardPaymentFields.
     */
    private fun buildCardPaymentFields(json: JsonObject): Ticket.TicketRequest.Payment.CardPaymentFields {
        val builder = Ticket.TicketRequest.Payment.CardPaymentFields.newBuilder()
        readString(json, "posTerminalId")?.let { builder.setPosTerminalId(it) }
        readString(json, "posCardType")?.let { builder.setPosCardType(it) }
        readInt(json, "posAutorizationCode")?.let { builder.setPosAutorizationCode(it) }
        readLong(json, "posRrn")?.let { builder.setPosRrn(it) }
        readInt(json, "posReceiptNumber")?.let { builder.setPosReceiptNumber(it) }
        return builder.build()
    }

    /**
     * Строит MobilePaymentFields.
     */
    private fun buildMobilePaymentFields(json: JsonObject): Ticket.TicketRequest.Payment.MobilePaymentFields {
        val builder = Ticket.TicketRequest.Payment.MobilePaymentFields.newBuilder()
        readString(json, "qrType")?.let { builder.setQrType(it) }
        readString(json, "qrId")?.let { builder.setQrId(it) }
        return builder.build()
    }

    /**
     * Строит Amounts для TicketRequest.
     */
    private fun buildAmounts(amountsJson: JsonObject): Ticket.TicketRequest.Amounts {
        val builder = Ticket.TicketRequest.Amounts.newBuilder()
        builder.setTotal(moneyBuilder.build(readObjectRequired(amountsJson, "total")))
        readObject(amountsJson, "taken")?.let { builder.setTaken(moneyBuilder.build(it)) }
        readObject(amountsJson, "change")?.let { builder.setChange(moneyBuilder.build(it)) }
        readObject(amountsJson, "markup")?.let { builder.setMarkup(buildModifier(it)) }
        readObject(amountsJson, "discount")?.let { builder.setDiscount(buildModifier(it)) }
        return builder.build()
    }

    /**
     * Строит ExtensionOptions для TicketRequest.
     */
    private fun buildExtensionOptions(json: JsonObject): Ticket.TicketRequest.ExtensionOptions {
        val builder = Ticket.TicketRequest.ExtensionOptions.newBuilder()
        readString(json, "customerEmail")?.let { builder.setCustomerEmail(it) }
        readString(json, "customerPhone")?.let { builder.setCustomerPhone(it) }
        readString(json, "customerIinOrBin")?.let { builder.setCustomerIinOrBin(it) }
        return builder.build()
    }

    /**
     * Строит ParentTicket для TicketRequest.
     */
    private fun buildParentTicket(json: JsonObject): Ticket.TicketRequest.ParentTicket {
        return Ticket.TicketRequest.ParentTicket.newBuilder()
            .setParentTicketNumber(readStringRequired(json, "parentTicketNumber"))
            .setParentTicketDataTime(dateTimeBuilder.build(json, "parentTicketDateTime"))
            .setKgdKkmId(readStringRequired(json, "kgdKkmId"))
            .setParentTicketTotal(moneyBuilder.build(readObjectRequired(json, "parentTicketTotal")))
            .setParentTicketIsOffline(readBoolRequired(json, "parentTicketIsOffline"))
            .build()
    }

    /**
     * Читает объект, если поле присутствует.
     */
    private fun readObject(json: JsonObject, key: String): JsonObject? {
        return json[key] as? JsonObject
    }

    /**
     * Читает обязательный объект или выбрасывает ошибку.
     */
    private fun readObjectRequired(json: JsonObject, key: String): JsonObject {
        return readObject(json, key) ?: throw IllegalArgumentException("Missing $key")
    }

    /**
     * Читает массив, если поле присутствует.
     */
    private fun readArray(json: JsonObject, key: String): JsonArray? {
        return json[key] as? JsonArray
    }

    /**
     * Читает обязательный массив или выбрасывает ошибку.
     */
    private fun readArrayRequired(json: JsonObject, key: String): JsonArray {
        return readArray(json, key) ?: throw IllegalArgumentException("Missing $key")
    }

    /**
     * Читает строку, если поле присутствует.
     */
    private fun readString(json: JsonObject, key: String): String? {
        val element = json[key] as? JsonPrimitive ?: return null
        return if (element.isString) element.content else null
    }

    /**
     * Читает обязательную строку или выбрасывает ошибку.
     */
    private fun readStringRequired(json: JsonObject, key: String): String {
        return readString(json, key) ?: throw IllegalArgumentException("Missing $key")
    }

    /**
     * Читает int, если поле присутствует.
     */
    private fun readInt(json: JsonObject, key: String): Int? {
        val element = json[key] as? JsonPrimitive ?: return null
        return element.intOrNull
    }

    /**
     * Читает обязательный int или выбрасывает ошибку.
     */
    private fun readIntRequired(json: JsonObject, key: String): Int {
        return readInt(json, key) ?: throw IllegalArgumentException("Missing $key")
    }

    /**
     * Читает long, если поле присутствует.
     */
    private fun readLong(json: JsonObject, key: String): Long? {
        val element = json[key] as? JsonPrimitive ?: return null
        return element.longOrNull
    }

    /**
     * Читает обязательный long или выбрасывает ошибку.
     */
    private fun readLongRequired(json: JsonObject, key: String): Long {
        return readLong(json, key) ?: throw IllegalArgumentException("Missing $key")
    }

    /**
     * Читает boolean, если поле присутствует.
     */
    private fun readBool(json: JsonObject, key: String): Boolean? {
        val element = json[key] as? JsonPrimitive ?: return null
        return element.booleanOrNull
    }

    /**
     * Читает обязательный boolean или выбрасывает ошибку.
     */
    private fun readBoolRequired(json: JsonObject, key: String): Boolean {
        return readBool(json, key) ?: throw IllegalArgumentException("Missing $key")
    }

    /**
     * Читает строку из элемента массива.
     */
    private fun readStringElement(element: Any?): String {
        val primitive = element as? JsonPrimitive
            ?: throw IllegalArgumentException("Invalid list value")
        require(primitive.isString) { "Invalid list value" }
        return primitive.content
    }

    /**
     * Проверяет, что элемент массива является объектом.
     */
    private fun requireObject(element: Any?, key: String): JsonObject {
        return element as? JsonObject ?: throw IllegalArgumentException("Invalid $key element")
    }
}
