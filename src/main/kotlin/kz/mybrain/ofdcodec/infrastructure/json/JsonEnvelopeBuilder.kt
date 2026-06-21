package kz.mybrain.ofdcodec.infrastructure.json

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kz.mybrain.ofdcodec.domain.model.CommandType
import kz.mybrain.ofdcodec.domain.model.MessageHeader
import kz.mybrain.ofdcodec.domain.model.MessageType
import kz.mybrain.ofdcodec.infrastructure.util.ProtocolVersion

object JsonEnvelopeBuilder {
    /**
     * Формирует JSON-конверт для ответа.
     */
    fun build(
        ofdId: String,
        messageType: MessageType,
        commandType: CommandType,
        header: MessageHeader,
        payload: JsonObject
    ): JsonObject {
        return buildJsonObject {
            put(JsonKeys.OFD_ID, JsonPrimitive(ofdId))
            put(JsonKeys.PROTOCOL_VERSION, JsonPrimitive(ProtocolVersion.toNumericString(header.protocolVersion)))
            put(JsonKeys.MESSAGE_TYPE, JsonPrimitive(messageType.name))
            put(JsonKeys.COMMAND_TYPE, JsonPrimitive(commandType.name))
            put(
                JsonKeys.HEADER,
                buildJsonObject {
                    put(JsonKeys.SIZE, JsonPrimitive(header.size))
                    put(JsonKeys.DEVICE_ID, JsonPrimitive(header.deviceId))
                    put(JsonKeys.TOKEN, JsonPrimitive(header.token))
                    put(JsonKeys.REQ_NUM, JsonPrimitive(header.reqNum))
                }
            )
            put(JsonKeys.PAYLOAD, payload)
        }
    }
}
