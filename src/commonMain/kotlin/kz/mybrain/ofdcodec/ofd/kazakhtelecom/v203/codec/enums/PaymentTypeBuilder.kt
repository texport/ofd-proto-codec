package kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.codec.enums

import kotlinx.serialization.json.JsonObject
import kz.kazakhtelecom.proto.v203.*
import kz.mybrain.ofdcodec.infrastructure.json.readString

/**
 * Чтение PaymentTypeEnum из JSON.
 */
internal class PaymentTypeBuilder {
    /**
     * Читает PaymentTypeEnum по ключу и возвращает его значение.
     */
    fun readRequired(json: JsonObject, key: String): PaymentTypeEnum {
        val value = json.readString(key)
        return PaymentTypeEnum.valueOf(
            value ?: throw IllegalArgumentException("Missing $key / Отсутствует $key / $key өрісі жетіспейді")
        )
    }
}
