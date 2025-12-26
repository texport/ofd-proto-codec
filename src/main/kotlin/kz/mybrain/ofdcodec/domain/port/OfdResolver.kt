package kz.mybrain.ofdcodec.domain.port

import kz.mybrain.ofdcodec.domain.registry.OfdRegistry
import kz.mybrain.ofdcodec.domain.model.MessageHeader

/**
 * Определение ОФД при декодировании ответа.
 */
fun interface OfdResolver {
    fun resolve(header: MessageHeader, payload: ByteArray, registry: OfdRegistry): String?
}
