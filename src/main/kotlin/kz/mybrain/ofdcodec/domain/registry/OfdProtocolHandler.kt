package kz.mybrain.ofdcodec.domain.registry

import kz.mybrain.ofdcodec.domain.port.Deserializer
import kz.mybrain.ofdcodec.domain.port.Serializer
import kz.mybrain.ofdcodec.domain.port.Validator

/**
 * Набор обработчиков для конкретного ОФД и версии протокола.
 */
internal data class OfdProtocolHandler(
    val ofdId: String,
    val protocolVersion: String,
    val requestValidator: Validator,
    val requestSerializer: Serializer,
    val responseValidator: Validator,
    val responseDeserializer: Deserializer
)
