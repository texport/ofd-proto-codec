package kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.codec.command

import kz.kazakhtelecom.proto.v203.Message
import kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.codec.service.ServiceRequestBuilder
import kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.codec.ticket.TicketRequestBuilder
import kotlinx.serialization.json.JsonObject

/**
 * Построение запроса COMMAND_TICKET.
 */
class CommandTicketRequestBuilder : CommandRequestBuilder {
    private val serviceRequestBuilder = ServiceRequestBuilder()
    private val ticketRequestBuilder = TicketRequestBuilder()

    /**
     * Собирает Message.Request для команды COMMAND_TICKET.
     */
    override fun build(json: JsonObject): Message.Request {
        val serviceRequest = serviceRequestBuilder.build(json)
        val ticketRequest = ticketRequestBuilder.build(json)

        return Message.Request.newBuilder()
            .setCommand(Message.CommandTypeEnum.COMMAND_TICKET)
            .setTicket(ticketRequest)
            .setService(serviceRequest)
            .build()
    }
}
