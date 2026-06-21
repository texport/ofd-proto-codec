package kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.codec.command

import kotlinx.serialization.json.JsonObject
import kz.kazakhtelecom.proto.v203.Message
import kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.codec.moneyplacement.MoneyPlacementRequestBuilder
import kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.codec.service.ServiceRequestBuilder

/**
 * Построение запроса COMMAND_MONEY_PLACEMENT.
 */
class CommandMoneyPlacementRequestBuilder : CommandRequestBuilder {
    private val serviceRequestBuilder = ServiceRequestBuilder()
    private val moneyPlacementRequestBuilder = MoneyPlacementRequestBuilder()

    /**
     * Собирает Message.Request для команды COMMAND_MONEY_PLACEMENT.
     */
    override fun build(json: JsonObject): Message.Request {
        val serviceRequest = serviceRequestBuilder.build(json)
        val moneyPlacementRequest = moneyPlacementRequestBuilder.build(json)

        return Message.Request.newBuilder()
            .setCommand(Message.CommandTypeEnum.COMMAND_MONEY_PLACEMENT)
            .setMoneyPlacement(moneyPlacementRequest)
            .setService(serviceRequest)
            .build()
    }
}
