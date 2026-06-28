package kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.codec

import kz.kazakhtelecom.proto.v203.*

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject

internal fun buildMoney(money: Money): JsonObject {
    return buildJsonObject {
        put("bills", JsonPrimitive(money.bills))
        put("coins", JsonPrimitive(money.coins))
    }
}

internal fun buildDateTime(dateTime: DateTime): JsonObject {
    return buildJsonObject {
        put(
            "date",
            buildJsonObject {
                put("year", JsonPrimitive(dateTime.date.year))
                put("month", JsonPrimitive(dateTime.date.month))
                put("day", JsonPrimitive(dateTime.date.day))
            }
        )
        put(
            "time",
            buildJsonObject {
                put("hour", JsonPrimitive(dateTime.time.hour))
                put("minute", JsonPrimitive(dateTime.time.minute))
                if (dateTime.time.second != null) {
                    put("second", JsonPrimitive(dateTime.time.second))
                }
            }
        )
    }
}
