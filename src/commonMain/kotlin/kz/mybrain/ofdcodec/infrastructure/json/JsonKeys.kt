package kz.mybrain.ofdcodec.infrastructure.json

/**
 * Ключи верхнего уровня для JSON-конверта.
 */
internal object JsonKeys {
    const val OFD_ID = "ofdId"
    const val PROTOCOL_VERSION = "protocolVersion"
    const val MESSAGE_TYPE = "messageType"
    const val COMMAND_TYPE = "commandType"
    const val HEADER = "header"
    const val PAYLOAD = "payload"
    const val DEVICE_ID = "deviceId"
    const val TOKEN = "token"
    const val REQ_NUM = "reqNum"
    const val SIZE = "size"
    const val MESSAGE_BASE64 = "messageBase64"
}
