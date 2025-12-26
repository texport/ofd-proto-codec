package kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.codec.enums

import kz.kazakhtelecom.proto.v203.Report
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * Чтение ReportTypeEnum из JSON.
 */
class ReportTypeBuilder {
    /**
     * Читает ReportTypeEnum по ключу и возвращает его значение.
     */
    fun readRequired(json: JsonObject, key: String): Report.ReportTypeEnum {
        val value = readString(json, key)
        return Report.ReportTypeEnum.valueOf(value ?: throw IllegalArgumentException("Missing $key"))
    }

    /**
     * Читает строку, если поле присутствует.
     */
    private fun readString(json: JsonObject, key: String): String? {
        val element = json[key] as? JsonPrimitive ?: return null
        return if (element.isString) element.content else null
    }
}
