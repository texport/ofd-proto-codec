package kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.codec.command

import kz.kazakhtelecom.proto.v203.Message
import kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.codec.nomenclature.NomenclatureRequestBuilder
import kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.codec.service.ServiceRequestBuilder
import kotlinx.serialization.json.JsonObject

/**
 * Сборщик Request для COMMAND_NOMENCLATURE.
 */
class CommandNomenclatureRequestBuilder : CommandRequestBuilder {
    private val serviceRequestBuilder = ServiceRequestBuilder()
    private val nomenclatureRequestBuilder = NomenclatureRequestBuilder()

    /**
     * Строит proto Request для COMMAND_NOMENCLATURE на основе JSON payload.
     */
    override fun build(json: JsonObject): Message.Request {
        val serviceRequest = serviceRequestBuilder.build(json)
        val nomenclatureRequest = nomenclatureRequestBuilder.build(json)
        return Message.Request.newBuilder()
            .setCommand(Message.CommandTypeEnum.COMMAND_NOMENCLATURE)
            .setService(serviceRequest)
            .setNomenclature(nomenclatureRequest)
            .build()
    }
}
