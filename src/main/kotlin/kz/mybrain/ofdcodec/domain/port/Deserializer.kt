package kz.mybrain.ofdcodec.domain.port

import kotlinx.serialization.json.JsonObject

/**
 * Десериализация бинарного payload в JSON согласно proto-библиотеке ОФД.
 */
internal interface Deserializer {
    fun deserialize(bytes: ByteArray): JsonObject
}
