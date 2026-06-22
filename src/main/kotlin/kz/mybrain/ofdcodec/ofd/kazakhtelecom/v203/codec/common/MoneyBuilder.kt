package kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.codec.common

import kotlinx.serialization.json.JsonObject
import kz.kazakhtelecom.proto.v203.Common
import kz.mybrain.ofdcodec.infrastructure.json.readIntRequired
import kz.mybrain.ofdcodec.infrastructure.json.readLongRequired

/**
 * Сборщик Money из JSON-структуры.
 */
internal class MoneyBuilder {
    /**
     * Строит Money из JSON-объекта.
     */
    fun build(sumJson: JsonObject): Common.Money {
        val bills = sumJson.readLongRequired("bills")
        val coins = sumJson.readIntRequired("coins")
        return Common.Money.newBuilder()
            .setBills(bills)
            .setCoins(coins)
            .build()
    }
}
