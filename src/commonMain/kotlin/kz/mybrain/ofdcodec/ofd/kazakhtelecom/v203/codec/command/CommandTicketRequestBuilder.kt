package kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.codec.command

import kotlinx.serialization.json.JsonObject
import kz.kazakhtelecom.proto.v203.*
import kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.codec.service.ServiceRequestBuilder
import kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.codec.ticket.TicketRequestBuilder

/**
 * Построение запроса COMMAND_TICKET.
 */
internal class CommandTicketRequestBuilder : CommandRequestBuilder {
    private val serviceRequestBuilder = ServiceRequestBuilder()
    private val ticketRequestBuilder = TicketRequestBuilder()

    /**
     * Собирает Request для команды COMMAND_TICKET.
     */
    override fun build(json: JsonObject): Request {
        val serviceRequest = serviceRequestBuilder.build(json)
        val ticketRequest = ticketRequestBuilder.build(json)

        return Request(
            command = CommandTypeEnum.COMMAND_TICKET,
            ticket = ticketRequest,
            service = serviceRequest
        )
    }
}
