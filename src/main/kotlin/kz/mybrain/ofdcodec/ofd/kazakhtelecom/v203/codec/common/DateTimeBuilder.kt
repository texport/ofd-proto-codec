package kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.codec.common

import kz.kazakhtelecom.proto.v203.Common
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull

/**
 * Сборщик proto DateTime из JSON-структуры.
 */
class DateTimeBuilder {
    /**
     * Строит DateTime по ключу в JSON-объекте.
     */
    fun build(container: JsonObject, key: String): Common.DateTime {
        val dt = container[key] as? JsonObject ?: throw IllegalArgumentException("Missing $key")
        val date = dt["date"] as? JsonObject ?: throw IllegalArgumentException("Missing $key.date")
        val time = dt["time"] as? JsonObject ?: throw IllegalArgumentException("Missing $key.time")

        val year = readUIntRequired(date, "year")
        val month = readUIntRequired(date, "month")
        val day = readUIntRequired(date, "day")
        val hour = readUIntRequired(time, "hour")
        val minute = readUIntRequired(time, "minute")
        val second = readUInt(time, "second")

        val dateProto = Common.Date.newBuilder()
            .setYear(year)
            .setMonth(month)
            .setDay(day)
            .build()

        val timeProto = Common.Time.newBuilder()
            .setHour(hour)
            .setMinute(minute)
            .apply { if (second != null) setSecond(second) }
            .build()

        return Common.DateTime.newBuilder()
            .setDate(dateProto)
            .setTime(timeProto)
            .build()
    }

    /**
     * Читает целое значение, если оно корректно.
     */
    private fun readUInt(json: JsonObject, key: String): Int? {
        val element = json[key] as? JsonPrimitive ?: return null
        return element.intOrNull
    }

    /**
     * Читает обязательное целое значение или выбрасывает ошибку.
     */
    private fun readUIntRequired(json: JsonObject, key: String): Int {
        return readUInt(json, key) ?: throw IllegalArgumentException("Missing $key")
    }
}
