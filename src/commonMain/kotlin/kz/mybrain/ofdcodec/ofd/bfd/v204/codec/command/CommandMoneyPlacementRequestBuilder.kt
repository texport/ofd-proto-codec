package kz.mybrain.ofdcodec.ofd.bfd.v204.codec.command

import kotlinx.serialization.json.JsonObject
import kz.bfd.proto.v204.CommandTypeEnum
import kz.bfd.proto.v204.Request
import kz.mybrain.ofdcodec.ofd.bfd.v204.codec.moneyplacement.MoneyPlacementRequestBuilder
import kz.mybrain.ofdcodec.ofd.bfd.v204.codec.service.ServiceRequestBuilder

/**
 * Построение запроса COMMAND_MONEY_PLACEMENT.
 */
internal class CommandMoneyPlacementRequestBuilder : CommandRequestBuilder {
    private val serviceRequestBuilder = ServiceRequestBuilder()
    private val moneyPlacementRequestBuilder = MoneyPlacementRequestBuilder()

    /**
     * Собирает Request для команды COMMAND_MONEY_PLACEMENT.
     */
    override fun build(json: JsonObject): Request {
        val serviceRequest = serviceRequestBuilder.build(json)
        val moneyPlacementRequest = moneyPlacementRequestBuilder.build(json)

        return Request(
            command = CommandTypeEnum.COMMAND_MONEY_PLACEMENT,
            money_placement = moneyPlacementRequest,
            service = serviceRequest
        )
    }
}
