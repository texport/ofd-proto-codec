package kz.mybrain.ofdcodec.ofd.bfd.v204.codec.enums

import kotlinx.serialization.json.JsonObject
import kz.bfd.proto.v204.ReportTypeEnum
import kz.mybrain.ofdcodec.infrastructure.json.readString

/**
 * Чтение ReportTypeEnum из JSON.
 */
internal class ReportTypeBuilder {
    /**
     * Читает ReportTypeEnum по ключу и возвращает его значение.
     */
    fun readRequired(json: JsonObject, key: String): ReportTypeEnum {
        val value = json.readString(key)
        return ReportTypeEnum.valueOf(
            value ?: throw IllegalArgumentException("Missing $key / Отсутствует $key / $key өрісі жетіспейді")
        )
    }
}
