package kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.codec.enums

import kotlinx.serialization.json.JsonObject
import kz.kazakhtelecom.proto.v203.Report
import kz.mybrain.ofdcodec.infrastructure.json.readString

/**
 * Чтение ReportTypeEnum из JSON.
 */
internal class ReportTypeBuilder {
    /**
     * Читает ReportTypeEnum по ключу и возвращает его значение.
     */
    fun readRequired(json: JsonObject, key: String): Report.ReportTypeEnum {
        val value = json.readString(key)
        return Report.ReportTypeEnum.valueOf(
            value ?: throw IllegalArgumentException("Missing $key / Отсутствует $key / $key өрісі жетіспейді")
        )
    }
}
