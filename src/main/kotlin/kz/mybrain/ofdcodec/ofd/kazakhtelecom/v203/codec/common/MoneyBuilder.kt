package kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.codec.common

import kz.kazakhtelecom.proto.v203.Common
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.intOrNull

/**
 * Сборщик Money из JSON-структуры.
 */
class MoneyBuilder {
    /**
     * Строит Money из JSON-объекта.
     */
    fun build(sumJson: JsonObject): Common.Money {
        val bills = readLongRequired(sumJson, "bills")
        val coins = readIntRequired(sumJson, "coins")
        return Common.Money.newBuilder()
            .setBills(bills)
            .setCoins(coins)
            .build()
    }

    /**
     * Читает long, если поле присутствует.
     */
    private fun readLong(json: JsonObject, key: String): Long? {
        val element = json[key] as? JsonPrimitive ?: return null
        return element.longOrNull
    }

    /**
     * Читает обязательный long или выбрасывает ошибку.
     */
    private fun readLongRequired(json: JsonObject, key: String): Long {
        val value = readLong(json, key)
        return value ?: throw IllegalArgumentException("Missing $key")
    }

    /**
     * Читает int, если поле присутствует.
     */
    private fun readInt(json: JsonObject, key: String): Int? {
        val element = json[key] as? JsonPrimitive ?: return null
        return element.intOrNull
    }

    /**
     * Читает обязательный int или выбрасывает ошибку.
     */
    private fun readIntRequired(json: JsonObject, key: String): Int {
        val value = readInt(json, key)
        return value ?: throw IllegalArgumentException("Missing $key")
    }
}
