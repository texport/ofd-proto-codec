package kz.mybrain.ofdcodec.ofd.bfd.v204.codec.common

import kotlinx.serialization.json.JsonObject
import kz.bfd.proto.v204.Money
import kz.mybrain.ofdcodec.infrastructure.json.readIntRequired
import kz.mybrain.ofdcodec.infrastructure.json.readLongRequired

/**
 * Сборщик Money из JSON-структуры.
 */
internal class MoneyBuilder {
    /**
     * Строит Money из JSON-объекта.
     */
    fun build(sumJson: JsonObject): Money {
        val bills = sumJson.readLongRequired("bills")
        val coins = sumJson.readIntRequired("coins")
        return Money(
            bills = bills,
            coins = coins
        )
    }
}
