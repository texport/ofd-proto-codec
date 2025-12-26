package kz.mybrain.ofdcodec.domain.port

import kotlinx.serialization.json.JsonObject

/**
 * Десериализация бинарного payload в JSON согласно proto-библиотеке ОФД.
 */
interface Deserializer {
    fun deserialize(bytes: ByteArray): JsonObject
}
