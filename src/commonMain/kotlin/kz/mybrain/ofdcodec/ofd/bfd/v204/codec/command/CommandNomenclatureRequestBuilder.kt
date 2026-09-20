package kz.mybrain.ofdcodec.ofd.bfd.v204.codec.command

import kotlinx.serialization.json.JsonObject
import kz.bfd.proto.v204.CommandTypeEnum
import kz.bfd.proto.v204.Request
import kz.mybrain.ofdcodec.ofd.bfd.v204.codec.nomenclature.NomenclatureRequestBuilder
import kz.mybrain.ofdcodec.ofd.bfd.v204.codec.service.ServiceRequestBuilder

/**
 * Сборщик Request для COMMAND_NOMENCLATURE.
 */
internal class CommandNomenclatureRequestBuilder : CommandRequestBuilder {
    private val serviceRequestBuilder = ServiceRequestBuilder()
    private val nomenclatureRequestBuilder = NomenclatureRequestBuilder()

    /**
     * Строит proto Request для COMMAND_NOMENCLATURE на основе JSON payload.
     */
    override fun build(json: JsonObject): Request {
        val serviceRequest = serviceRequestBuilder.build(json)
        val nomenclatureRequest = nomenclatureRequestBuilder.build(json)
        return Request(
            command = CommandTypeEnum.COMMAND_NOMENCLATURE,
            service = serviceRequest,
            nomenclature = nomenclatureRequest
        )
    }
}
