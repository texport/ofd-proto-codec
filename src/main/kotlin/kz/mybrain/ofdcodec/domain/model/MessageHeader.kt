package kz.mybrain.ofdcodec.domain.model

/**
 * Заголовок сообщения по протоколу ККМ ↔ ОФД.
 */
data class MessageHeader(
    val appCode: Int,
    val protocolVersion: Int,
    val size: Long,
    val deviceId: Long,
    val token: Long,
    val reqNum: Int
)

/**
 * Константы заголовка.
 */
object HeaderConstants {
    const val APPCODE = 0x81A2
    const val HEADER_SIZE = 18
}
