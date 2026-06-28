package kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.codec.common

import kotlinx.serialization.json.JsonObject
import kz.kazakhtelecom.proto.v203.*
import kz.mybrain.ofdcodec.infrastructure.json.readInt
import kz.mybrain.ofdcodec.infrastructure.json.readIntRequired

/**
 * Сборщик proto DateTime из JSON-структуры.
 */
internal class DateTimeBuilder {
    /**
     * Строит DateTime по ключу в JSON-объекте.
     */
    fun build(container: JsonObject, key: String): DateTime {
        val dt = container[key] as? JsonObject
            ?: throw IllegalArgumentException("Missing $key / Отсутствует $key / $key өрісі жетіспейді")
        val date = dt["date"] as? JsonObject
            ?: throw IllegalArgumentException("Missing $key.date / Отсутствует $key.date / $key.date өрісі жетіспейді")
        val time = dt["time"] as? JsonObject
            ?: throw IllegalArgumentException("Missing $key.time / Отсутствует $key.time / $key.time өрісі жетіспейді")

        val year = date.readIntRequired("year")
        val month = date.readIntRequired("month")
        val day = date.readIntRequired("day")
        val hour = time.readIntRequired("hour")
        val minute = time.readIntRequired("minute")
        val second = time.readInt("second")

        val dateProto = Date(
            year = year,
            month = month,
            day = day
        )

        val timeProto = Time(
            hour = hour,
            minute = minute,
            second = second
        )

        return DateTime(
            date = dateProto,
            time = timeProto
        )
    }
}
